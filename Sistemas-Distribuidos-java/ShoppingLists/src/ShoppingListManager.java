import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


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

    public void addShoppingList(ShoppingList shoppingList){
        shoppingLists.add(shoppingList);
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

    public void saveShoppingListToFile(ShoppingList shoppingList, String filePath) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            // Write the shopping list name to the file
            writer.println(shoppingList.getName());

            // Write each shopping item to the file
            for (Item item : shoppingList.getItems()) {
                writer.println(item.getName() + "," + item.getQuantity());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }



    public void loadShoppingLists() {
        // Implement loading shopping lists from files
    }

    public ShoppingList loadShoppingListFromFile(String filePath) {
        ShoppingList shoppingList = new ShoppingList();

        try (Scanner scanner = new Scanner(new File(filePath))) {
            // Read the shopping list name
            if (scanner.hasNextLine()) {
                shoppingList.setName(scanner.nextLine());
            }

            // Read and add each shopping item
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    String itemName = parts[0];
                    int itemQuantity = Integer.parseInt(parts[1]);
                    shoppingList.addItem(new Item(itemName, itemQuantity));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return shoppingList;
    }

}
