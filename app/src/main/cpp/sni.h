#ifndef DPITUNNEL_SNI_H
#define DPITUNNEL_SNI_H

#include <tlse.h>
#include <string>

int recv_string_tls(int & socket, SSL *context, std::string & message, unsigned int & last_char);
int send_string_tls(int & socket, TLSContext *context, const std::string & string_to_send, unsigned int last_char);
SSL* init_tls_server_server(const std::string & sni_str, const std::vector<std::string> & sni_arr);
SSL* init_tls_server_client(int & client_socket, SSL* server_context);
SSL* init_tls_client(int & client_socket, std::string & sni);

#endif //DPITUNNEL_SNI_H
