#include "client.h"
#include "shoppinglist.h"
#include <iostream>
#include <filesystem>  // C++17 filesystem library


namespace fs = std::filesystem;
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

int Client::loadExistingLists() {


    std::string directoryPath = "shopping_lists/";

    if (fs::exists(directoryPath) && fs::is_directory(directoryPath)) {
        for (const fs::directory_entry& entry : fs::directory_iterator(directoryPath)) {
            std::string fileName = entry.path().filename().string();
            size_t found = fileName.find("user_" + std::to_string(userId) + "_shopping_list_");
            if (found != std::string::npos) {
                // Load and display the shopping list
                ShoppingList loadedList = loadShoppingListFromJson(entry.path().string());

                std::cout << "Loaded Shopping List: " << loadedList.name << "\n";
                for (size_t i = 0; i < loadedList.items.size(); ++i) {
                    const string& item = loadedList.items[i];
                    std::cout << "Item " << i << ": " << item.name << ", Quantity: " << item.quantity << "\n";
                }
            }
        }
    } else {
        std::cerr << "Error: Shopping list directory not found.\n";
    }
    return 0;
}

 
