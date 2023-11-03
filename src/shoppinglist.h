#ifndef SHOPPINGLIST_H
#define SHOPPINGLIST_H

#include <vector>
#include <string>

class ShoppingList {
public:
    void addItem(const std::string& item);
    void clearList();
    void displayList() const;

private:
    std::vector<std::string> items;
};

class ShoppingListManager {
public:
    void createList();
    void addItemToList(const std::string& item, int listIndex);
    void clearList(int listIndex);
    void displayList(int listIndex) const;
    int getNumLists();

private:
    std::vector<ShoppingList> lists;
};

#endif // SHOPPINGLIST_H
