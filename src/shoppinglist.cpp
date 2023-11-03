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

void ShoppingListManager::createList() {
    lists.push_back(ShoppingList());
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

int ShoppingListManager::getNumLists()
{
    return this->lists.size();
}
