import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ShoppingListManager {
    private List<ShoppingList> shoppingLists;

    public ShoppingListManager() {
        this.shoppingLists = new ArrayList<>();
        loadShoppingLists();
    }

    public void createShoppingList(String listName) {
        ShoppingList shoppingList = new ShoppingList(listName);
        shoppingLists.add(shoppingList);
        System.out.println("Shopping list created: " + listName);
    }

    public void deleteShoppingList(String listName){
        List<ShoppingList> lists = this.shoppingLists;

        for (int i = 0; i < lists.size(); i++) {
            if(lists.get(i).getName().equals(listName)){
                lists.remove(i);
                break;

            }
        }
    }

    public List<ShoppingList> getShoppingLists() {
        return shoppingLists;
    }

    public void displayShoppingLists() {
        System.out.println("Shopping Lists:");
        List<ShoppingList> lists = this.shoppingLists;
        for (int i = 0; i < lists.size(); i++) {
            System.out.println((i + 1) + ". " + lists.get(i).getName());
        }
    }

    public void saveShoppingLists() {
        // Implement saving shopping lists to files
    }

    public void loadShoppingLists() {
        // Implement loading shopping lists from files
    }
}
