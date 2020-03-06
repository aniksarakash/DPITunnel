package ru.evgeniy.dpitunnel;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.DataOutputStream;

public class NativeService extends Service {

    private SharedPreferences prefs;
    private static int FOREGROUND_ID = 97456;
    public static final String CHANNEL_ID = "DPITunnelChannel";
    public static final String ACTION_STOP = "ru.evgeniy.dpitunnel.ACTION_STOP";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    thread nativeThread = new thread();
    @Override
    public void onCreate() {
        String log_tag = "Java/NativeService/onCreate";

        // Start foreground service
        createNotificationChannel();

        // Add stop service button
        Intent intent1 = new Intent();
        intent1.setAction(ACTION_STOP);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(this, 0, intent1, 0);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_is_running))
                .setSmallIcon(R.mipmap.ic_notification_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_off_button, getText(R.string.off), pendingIntent1)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification notification = builder.build();

        // Show notification
        startForeground(FOREGROUND_ID, notification);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set path to hostlist file
        setHostlistPath(prefs.getString("other_hostlist_path", null));

        // Start native code
        nativeThread.start();

        // Set http_proxy settings if need
        if(prefs.getBoolean("other_proxy_setting", false)) {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                outputStream.writeBytes("settings put global http_proxy 127.0.0.1:" + prefs.getString("other_bind_port", null) + "\n");
                outputStream.flush();

                outputStream.writeBytes("exit\n");
                outputStream.flush();

                su.waitFor();
            } catch (Exception e) {
                Log.e(log_tag, "Failed to set http_proxy global settings");
            }
        }

        // Add receiver to receive notification events
        BroadcastReceiver receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();

                if (action.equals(ACTION_STOP))
                {
                    stopSelf();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);


        // Inform app what service is started
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction("LOGO_BUTTON_ON");
        sendBroadcast(broadCastIntent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        createNotificationChannel();

        // Add stop service button
        Intent intent1 = new Intent();
        intent1.setAction(ACTION_STOP);
        PendingIntent pendingIntent1 = PendingIntent.getBroadcast(this, 0, intent1, 0);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_is_running))
                .setSmallIcon(R.mipmap.ic_notification_logo)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(R.drawable.ic_off_button, getText(R.string.off), pendingIntent1)
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        Notification notification = builder.build();

        // Show notification
        startForeground(FOREGROUND_ID, notification);

        return START_NOT_STICKY;
    }


    private class thread extends Thread{
        String log_tag = "Java/NativeService/nativeThread";

        volatile boolean isRunning = true;
        @Override
        public void run() {
            if(init(PreferenceManager.getDefaultSharedPreferences(NativeService.this)) == -1)
            {
                Log.e(log_tag, "Init failure");
                NativeService.this.stopSelf();
                return;
            }

            while(isRunning)
            {
                acceptClient();
            }
        }

        public void quit() {
            isRunning = false;
            deInit();
        }
    }

    @Override
    public void onDestroy() {
        String log_tag = "Java/NativeService/onDestroy";

        // Unset http_proxy settings if need
        if(prefs.getBoolean("other_proxy_setting", false)) {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

                outputStream.writeBytes("settings put global http_proxy :0\n");
                outputStream.flush();

                outputStream.writeBytes("exit\n");
                outputStream.flush();

                su.waitFor();
            } catch (Exception e) {
                Log.e(log_tag, "Failed to unset http_proxy global settings");
            }
        }

        nativeThread.quit();

        // Inform app what service is stopped
        Intent broadCastIntent = new Intent();
        broadCastIntent.setAction("LOGO_BUTTON_OFF");
        sendBroadcast(broadCastIntent);
    }

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = CHANNEL_ID;
            String description = CHANNEL_ID;
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    static {
        System.loadLibrary("dpi-bypass");
    }

    public native int init(SharedPreferences prefs);
    public native void acceptClient();
    public native void deInit();
    public native void setHostlistPath(String ApplicationDirectory);
}
