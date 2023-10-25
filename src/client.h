#pragma once
#include <zmq.hpp>

class Client {
public:
    Client();
    void setField(const std::string& value);
    std::string getField();

private:
    zmq::context_t context;
    zmq::socket_t socket;
};
