#include "db.h"
#include "MiddleManServer.h"
#include "client.h"
#include <iostream>
#include <thread>

using namespace std;

int main()
{
    /*
    DBShard db;
    Server server(db);
    
    thread serverThread([&server]()
                             { server.run(); });
    */
    

    Client client;
    client.displayUI();

    /*
    client.setField("Hello from client");
    std::cout << "Client got from server: " << client.getField() << std::endl;
    
    serverThread.join();
    */
    return 0;
}
