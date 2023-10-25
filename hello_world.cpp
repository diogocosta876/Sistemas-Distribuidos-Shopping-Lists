#include <zmq.hpp>
#include <iostream>
#include <string>

using namespace std;
using namespace zmq;

int main()
{
    context_t context(1);
    socket_t sock1(context, socket_type::req);

    // Store the last endpoint manually
    string lastEndpoint;

    // Connect to a remote endpoint
    lastEndpoint = "tcp://localhost:5555"; // Set your endpoint here
    sock1.connect(lastEndpoint);

    // Later in your code, if you need to access the last endpoint:
    cout << "Last endpoint: " << lastEndpoint << endl;

    return 0;
}