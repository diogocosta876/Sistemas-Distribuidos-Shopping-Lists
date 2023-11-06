#include "MiddleManServer.h"
#include <iostream>
using namespace std;

Server::Server()
    : context(1), socket(context, zmq::socket_type::rep)
{
    socket.bind("tcp://*:5555");
}

void Server::run()
{
    while (true)
    {
        zmq::message_t request;
        socket.recv(request);
        string cmd = string(static_cast<char *>(request.data()), request.size());

        if (cmd == "GET")
        {
            string data = "gk";
            zmq::message_t reply(data.size());
            memcpy(reply.data(), data.c_str(), data.size());
            socket.send(reply, zmq::send_flags::none);
        }
        else
        {
            // Send a reply to acknowledge the setField command
            zmq::message_t reply(2); // "OK" as an acknowledgment
            memcpy(reply.data(), "OK", 2);
            socket.send(reply, zmq::send_flags::none);
        }
    }
}