package ru.evgeniy.dpitunnel;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;

import org.xbill.DNS.Message;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class LocalDNSServer extends Thread {

    private String log_tag = "Java/LocalDNSServer";

    private SharedPreferences prefs;
    private DatagramSocket serverSocket;
    private byte[] buf = new byte[256];
    private volatile boolean isRunning = true;

    static {
        System.loadLibrary("dpi-bypass");
    }

    LocalDNSServer(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            VpnService vpnService = new VpnService();
            serverSocket = new DatagramSocket(49150);
            vpnService.protect(serverSocket);
        } catch (Exception e) {
            Log.e(log_tag, "Failed to create server socket");
            e.printStackTrace();
        }
    }

    private byte[] makeDOHRequest(String doh_server_url, Message request)
    {
        String log_tag = "Java/LocalDNSServer/makeDOHReqst";

        byte[] response = null;
        try {
            // Encode DNS request to base64
            String dns_message_encoded = Base64.encodeToString(request.toWire(), Base64.NO_PADDING);

            System.setProperty("http.keepAlive", "false");
            System.setProperty("java.net.preferIPv4Stack" , "true");

            // Proper process test.com and test.com/dns-query urls

            // Remove '/' if it exists on string end
            if(doh_server_url.charAt(doh_server_url.length() - 1) == '/') doh_server_url = doh_server_url.substring(0, doh_server_url.length() - 1);

            // Append request
            String url_str;
            if(doh_server_url.substring(doh_server_url.length() - 9).equals("dns-query"))
                url_str = doh_server_url + "?dns=" + dns_message_encoded;
            else
                url_str = doh_server_url + "/?dns=" + dns_message_encoded;

            URL url = new URL(url_str);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(Proxy.NO_PROXY);

            // Add header
            connection.setRequestProperty("accept", "application/dns-message");

            // Create the SSL connection
            connection.setSSLSocketFactory(new NoSSLv3SocketFactory());

            // Set options and connect
            connection.setReadTimeout(700);
            connection.setConnectTimeout(700);
            connection.setRequestMethod("GET");
            connection.setDoInput(true);

            // Save
            InputStream in = connection.getInputStream();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }

            response = result.toByteArray();

        } catch (Exception e) {
            Log.e(log_tag, "DoH request failed");
            e.printStackTrace();
        }

        return response;
    }

    @Override
    public void run() {
        try {
            serverSocket.setSoTimeout(1000);
        } catch (Exception e) {
            Log.e(log_tag, "Failed to set server socket timeout");
        }
        while (isRunning) {
            try {
                // Receive packet
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                serverSocket.receive(packet);
                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();

                // Process request
                Message dnsRequestMessage = new Message(buf);

                // Resolve host
                byte[] response = null;
                boolean isOK = false;
                String[] dohServersUrls = prefs.getString("dns_doh_server", null).split("\n");
                for (String dohServerUrl : dohServersUrls) {
                    response = makeDOHRequest(dohServerUrl, dnsRequestMessage);
                    if(response == null)
                        Log.e(log_tag, "Failed to make request to DoH server. Trying again...");
                    else {
                        isOK = true;
                        break;
                    }
                }

                if (!isOK) {
                    Log.e(log_tag, "No request to the DoH servers was successful. Can't process client");
                    continue;
                }

                // Send response packet
                packet = new DatagramPacket(response, response.length, clientAddress, clientPort);
                serverSocket.send(packet);
            } catch (SocketTimeoutException e) {

            } catch (Exception e) {
                Log.e(log_tag, "Failed to process request");
                e.printStackTrace();
            }
        }
        serverSocket.close();
    }

    public void quit() {
        isRunning = false;
    }

}
