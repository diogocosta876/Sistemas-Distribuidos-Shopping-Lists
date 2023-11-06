#pragma once
#include "db.h"
#include <zmq.hpp>

class Server {
public:
    Server();
    void run();

private:
    zmq::context_t context;
    zmq::socket_t socket;
};
