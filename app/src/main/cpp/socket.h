#ifndef DPITUNNEL_SOCKET_H
#define DPITUNNEL_SOCKET_H

int recv_string(int & socket, std::string & message, unsigned int & last_char);
int recv_string(int & socket, std::string & message, struct timeval timeout, unsigned int & last_char);
int send_string(int & socket, const std::string & string_to_send, unsigned int last_char);
int send_string(int & socket, const std::string & string_to_send, unsigned int split_position, unsigned int last_char);
int init_remote_server_socket(int & remote_server_socket, std::string & remote_server_host, int remote_server_port, bool is_https, bool hostlist_condition, SSL *& client_context);

#endif //DPITUNNEL_SOCKET_H
