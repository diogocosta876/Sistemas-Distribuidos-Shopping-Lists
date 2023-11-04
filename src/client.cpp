#include "client.h"
#include "shoppinglist.h"
#include <iostream>

using namespace std;

Client::Client()
    : context(1), socket(context, zmq::socket_type::req)
{
    socket.connect("tcp://localhost:5555");
}

void Client::setField(const string &value)
{
    zmq::message_t message(value.size());
    memcpy(message.data(), value.c_str(), value.size());
    socket.send(message, zmq::send_flags::none);

    // Receive acknowledgment from server
    zmq::message_t reply;
    socket.recv(reply);
}

string Client::getField()
{
    zmq::message_t message("GET");
    socket.send(message, zmq::send_flags::none);

    zmq::message_t reply;
    socket.recv(reply);
    return string(static_cast<char *>(reply.data()), reply.size());
}

int Client::displayUI()
{
    
    ShoppingListManager listManager;
    int listIndex = -1;
    int choice;

    while (true) {

        cout << "Shopping List Application" << endl;

        if(listIndex != -1)
        {
            cout << "Number of lists: "<< listManager.getNumLists()<< endl;
            cout << "Selected List: "<<listManager.getListNameByIndex(listIndex)<< endl;
        }

        
        cout << "1. Create a new list" << endl;
        cout << "2. Add item to list" << endl;
        cout << "3. Clear list" << endl;
        cout << "4. Display list" << endl;
        cout << "5. Switch to another list" << endl;
        cout << "6. Exit" << endl;
        cout << "Enter your choice: ";
        cin >> choice;

        string input;
        if(listManager.getNumLists() == 0 && choice != 1 && choice != 6){
            
            cout << "You have no lists"<< endl;
            
        }else{
            if (choice == 1) {
                cout << "Enter your new list name: "<< endl;
                cin.ignore();
                getline(cin,input);
                listManager.createList(input);
                listIndex = listManager.getNumLists()-1;
            } else if (choice == 2) {
                
                
                cout << "Enter item to add: ";
                cin.ignore();
                getline(cin, input);
                listManager.addItemToList(input, listIndex);
            
                
            } else if (choice == 3) {
                listManager.clearList(listIndex);
            } else if (choice == 4) {
                listManager.displayList(listIndex);
            } else if (choice == 5) {
               
                cout << "Enter list index: ";
                cin >> listIndex;
                if (listIndex < 0 || listIndex >= listManager.getNumLists()) {
                    cout << "Invalid list index." << endl;
                    listIndex = 0;
                }
                
                
            } else if (choice == 6) {
                cout << "Terminating This user UI" << endl;
                break;
            } else {
                cout << "Invalid choice. Please try again" << endl;
            }
        }

        

        cout << endl;
        cout << endl;
    }

    return 0;
}

 
