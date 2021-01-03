#ifndef DPITUNNEL_HOSTLIST_H
#define DPITUNNEL_HOSTLIST_H

bool find_in_hostlist(const std::string& host);
bool find_in_hostlist(const std::vector<std::string>& host);
int parse_hostlist();

#endif //DPITUNNEL_HOSTLIST_H
