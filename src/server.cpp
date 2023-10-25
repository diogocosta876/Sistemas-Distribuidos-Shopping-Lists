#include "server.h"
#include <iostream>

Server::Server(DBShard &db)
    : dbShard(db), context(1), socket(context, zmq::socket_type::rep)
{
    socket.bind("tcp://*:5555");
}

void Server::run()
{
    while (true)
    {
        zmq::message_t request;
        socket.recv(request);
        std::string cmd = std::string(static_cast<char *>(request.data()), request.size());

        if (cmd == "GET")
        {
            std::string data = dbShard.getField();
            zmq::message_t reply(data.size());
            memcpy(reply.data(), data.c_str(), data.size());
            socket.send(reply, zmq::send_flags::none);
        }
        else
        {
            dbShard.setField(cmd);
        }
    }
}
