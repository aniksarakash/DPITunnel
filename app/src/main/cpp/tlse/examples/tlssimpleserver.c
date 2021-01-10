#include <stdio.h>
#include <string.h>    //strlen
#ifdef _WIN32
    #include <winsock2.h>
    #define socklen_t int
    #define sleep(x)    Sleep(x*1000)
#else
    #include <sys/socket.h>
    #include <arpa/inet.h>
#endif

#include "../tlse.c"

int main(int argc , char *argv[]) {
    int socket_desc , client_sock , read_size;
    socklen_t c;
    struct sockaddr_in server , client;
    char client_message[0xFFFF];
    const char msg[] = "HTTP/1.1 200 OK\r\nContent-length: 31\r\nContent-type: text/plain\r\n\r\nHello world from TLSe (TLS 1.2)";

#ifdef _WIN32
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#else
    signal(SIGPIPE, SIG_IGN);
#endif

    socket_desc = socket(AF_INET , SOCK_STREAM , 0);
    if (socket_desc == -1) {
        printf("Could not create socket");
        return 0;
    }
     
    server.sin_family = AF_INET;
    server.sin_addr.s_addr = INADDR_ANY;
    server.sin_port = htons(2000);
     
    int enable = 1;
    setsockopt(socket_desc, SOL_SOCKET, SO_REUSEADDR, &enable, sizeof(int));

    if( bind(socket_desc,(struct sockaddr *)&server , sizeof(server)) < 0) {
        perror("bind failed. Error");
        return 1;
    }
     
    listen(socket_desc , 3);
     
    c = sizeof(struct sockaddr_in);

    unsigned int size;

    SSL *server_ctx = SSL_CTX_new(SSLv3_server_method());
    if (!server_ctx) {
        fprintf(stderr, "Error creating server context\n");
        return -1;
    }
    SSL_CTX_use_certificate_file(server_ctx, "testcert/fullchain.pem", SSL_SERVER_RSA_CERT);
    SSL_CTX_use_PrivateKey_file(server_ctx, "testcert/privkey.pem", SSL_SERVER_RSA_KEY);

    if (!SSL_CTX_check_private_key(server_ctx)) {
        fprintf(stderr, "Private key not loaded\n");
        return -2;
    }

    while (1) {
        client_sock = accept(socket_desc, (struct sockaddr *)&client, &c);
        if (client_sock < 0) {
            fprintf(stderr, "Accept failed\n");
            return -3;
        }
        SSL *client = SSL_new(server_ctx);
        if (!client) {
            fprintf(stderr, "Error creating SSL client\n");
            return -4;
        }
        SSL_set_fd(client, client_sock);
        if (SSL_accept(client)) {
            fprintf(stderr, "Cipher %s\n", tls_cipher_name(client));
            while ((read_size = SSL_read(client, client_message, sizeof(client_message))) >= 0) {
                fwrite(client_message, read_size, 1, stdout);
                
                if (SSL_write(client, msg, strlen(msg)) < 0)
                    fprintf(stderr, "Error in SSL write\n");
                break;
            }
        } else
            fprintf(stderr, "Error in handshake\n");
        
        SSL_shutdown(client);
#ifdef __WIN32
        shutdown(client_sock, SD_BOTH);
        closesocket(client_sock);
#else
        shutdown(client_sock, SHUT_RDWR);
        close(client_sock);
#endif
        SSL_free(client);
    }
    SSL_CTX_free(server_ctx);
    return 0;
}
