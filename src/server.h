#pragma once
#include "db.h"
#include <zmq.hpp>

class Server {
public:
    Server(DBShard& db);
    void run();

private:
    DBShard& dbShard;
    zmq::context_t context;
    zmq::socket_t socket;
};
