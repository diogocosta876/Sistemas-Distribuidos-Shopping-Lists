#ifndef SHOPPINGLIST_H
#define SHOPPINGLIST_H

#include <vector>
#include <string>
using namespace std;

class ShoppingList {
public:
    void addItem(const string& item);
    void clearList();
    void displayList() const;
    string getListName();
    void setListName(string newName);

private:
    vector<string> items;
    string Name;
};

class ShoppingListManager {
public:
    void createList(string name);
    void addItemToList(const string& item, int listIndex);
    void clearList(int listIndex);
    void displayList(int listIndex) const;
    string getListNameByIndex(int listIndex);
    int getNumLists();

private:
    vector<ShoppingList> lists;
};

#endif // SHOPPINGLIST_H
