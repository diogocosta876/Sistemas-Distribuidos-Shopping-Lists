#include "shoppinglist.h"
#include <iostream>
using namespace std;

void ShoppingList::addItem(const string& item) {
    items.push_back(item);
}

void ShoppingList::clearList() {
    items.clear();
}

void ShoppingList::displayList() const {
    if (items.empty()) {
        cout << "Your shopping list is empty." << endl;
    } else {
        cout << "Shopping List:" << endl;
        for (const string& item : items) {
            cout << "- " << item << endl;
        }
    }
}

string ShoppingList::getListName()
{
    return this->Name;
}

void ShoppingList::setListName(string newName)
{
    this->Name = newName;
}

void ShoppingListManager::createList(string name) {
    ShoppingList new_ShoppingList = ShoppingList();
    new_ShoppingList.setListName(name);
    lists.push_back(new_ShoppingList);
}

void ShoppingListManager::addItemToList(const string& item, int listIndex) {
    if (listIndex >= 0 && listIndex < lists.size()) {
        lists[listIndex].addItem(item);
    } else {
        cout << "Invalid list index." << endl;
    }
}

void ShoppingListManager::clearList(int listIndex) {
    if (listIndex >= 0 && listIndex < lists.size()) {
        lists[listIndex].clearList();
        cout << "List cleared." << endl;
    } else {
        cout << "Invalid list index." << endl;
    }
}

void ShoppingListManager::displayList(int listIndex) const {
    if (listIndex >= 0 && listIndex < lists.size()) {
        cout << "List " << listIndex << ":" << endl;
        lists[listIndex].displayList();
    } else {
        cout << "Invalid list index." << endl;
    }
}

string ShoppingListManager::getListNameByIndex(int listIndex)
{
    return lists[listIndex].getListName();
}

int ShoppingListManager::getNumLists()
{
    return this->lists.size();
}
