#include "db.h"
#include "server.h"
#include "client.h"
#include <iostream>
#include <thread>

int main()
{
    DBShard db;
    Server server(db);

    std::thread serverThread([&server]()
                             { server.run(); });

    Client client;
    client.setField("Hello from client");
    std::cout << "Client got from server: " << client.getField() << std::endl;

    serverThread.join();
    return 0;
}
