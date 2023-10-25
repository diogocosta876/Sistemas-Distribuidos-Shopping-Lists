#include "client.h"

Client::Client()
    : context(1), socket(context, zmq::socket_type::req)
{
    socket.connect("tcp://localhost:5555");
}

void Client::setField(const std::string &value)
{
    zmq::message_t message(value.size());
    memcpy(message.data(), value.c_str(), value.size());
    socket.send(message, zmq::send_flags::none);
}

std::string Client::getField()
{
    zmq::message_t message("GET");
    socket.send(message, zmq::send_flags::none);

    zmq::message_t reply;
    socket.recv(reply);
    return std::string(static_cast<char *>(reply.data()), reply.size());
}
