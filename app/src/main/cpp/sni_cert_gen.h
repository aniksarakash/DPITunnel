#ifndef DPITUNNEL_SNI_CERT_GEN_H
#define DPITUNNEL_SNI_CERT_GEN_H
struct GeneratedCA
{
    std::string       public_key_pem;
    std::string       private_key_pem;
};

int generate_ssl_cert(const std::string & sni_str, const std::vector<std::string> & sni_arr, struct GeneratedCA & generatedCa);

#endif //DPITUNNEL_SNI_CERT_GEN_H
