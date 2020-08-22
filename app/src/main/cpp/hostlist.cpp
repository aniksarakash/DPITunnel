#include "dpi-bypass.h"
#include "hostlist.h"

extern struct Settings settings;
extern JavaVM* javaVm;
extern jclass localdnsserver_class;

std::vector<std::string> hostlist_vector;

bool find_in_hostlist(std::string host)
{
    std::string log_tag = "CPP/find_in_hostlist";

    // In VPN mode when connecting to https sites proxy server gets CONNECT requests to ip addresses
    // So if we receive ip address we need to find hostname for it

    // Check if host is IP
    struct sockaddr_in sa;
    int result = inet_pton(AF_INET, host.c_str(), &sa.sin_addr);
    if(settings.other.is_use_vpn && result != 0)
    {
        // Get JNIEnv
        JNIEnv* jni_env;
        javaVm->GetEnv((void**) &jni_env, JNI_VERSION_1_6);

        // Attach JNIEnv
        javaVm->AttachCurrentThread(&jni_env, NULL);

        // Find Java method
        jmethodID localdnsserver_get_hostname = jni_env->GetStaticMethodID(localdnsserver_class, "getHostname", "(Ljava/lang/String;)Ljava/lang/String;");
        if(localdnsserver_get_hostname == NULL)
        {
            log_error(log_tag.c_str(), "Failed to find getHostname method");
            return 0;
        }

        // Call Java method
        jstring response_string_object = (jstring) jni_env->CallStaticObjectMethod(localdnsserver_class, localdnsserver_get_hostname, jni_env->NewStringUTF(host.c_str()));
        host = jni_env->GetStringUTFChars(response_string_object, 0);
        if(host.empty())
        {
            log_error(log_tag.c_str(), "Failed to find hostname to ip");
            return 0;
        }

        // Detach thread
        javaVm->DetachCurrentThread();
    }

    for(std::string host_in_vector : hostlist_vector)
        if(host_in_vector == host)
        {
            log_debug(log_tag.c_str(), "Found host in hostlist. %s, %s", host_in_vector.c_str(), host.c_str());
            return 1;
        }

    return 0;
}

int parse_hostlist()
{
    std::string log_tag = "CPP/parse_hostlist";

    // Open hostlist file
    std::ifstream hostlist_file;
    hostlist_file.open(settings.hostlist.hostlist_path);
    if(!hostlist_file)
    {
        log_error(log_tag.c_str(), "Failed to open hostlist file");
        return -1;
    }

    // Create string object from hostlist file
    std::stringstream hostlist_stream;
    hostlist_stream << hostlist_file.rdbuf();
    std::string hostlist_string = hostlist_stream.str();

    // Parse hostlist file
    if(settings.hostlist.hostlist_format == "json")
    {
        // Parse with rapidjson
        rapidjson::Document hostlist_document;

        if(hostlist_document.Parse(hostlist_string.c_str()).HasParseError())
        {
            log_error(log_tag.c_str(), "Failed to parse hostlist file");
            return -1;
        }

        // Convert rapidjson::Document to vector<string>
        hostlist_vector.reserve(hostlist_document.GetArray().Size());
        for(const auto & host_in_list : hostlist_document.GetArray())
            hostlist_vector.push_back(host_in_list.GetString());
    }
    else if(settings.hostlist.hostlist_format == "txt")
    {
        // Parse as text
        char delimiter = '\n';
        std::string host;
        std::istringstream stream(hostlist_string);
        while (std::getline(stream, host, delimiter))
            hostlist_vector.push_back(host);
    }

    return 0;
}