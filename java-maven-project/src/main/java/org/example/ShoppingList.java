package org.example;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {
    private String name;
    private List<Item> items;

    private String filePath;

    public ShoppingList(String name) {
        this.name = name;
        this.items = new ArrayList<>();
        this.filePath = "./ShoppingLists/lists/";
    }

    public ShoppingList(){
        this.items = new ArrayList<>();
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }

    // Getters and setters

    public String getName() {
        return this.name;
    }

    public void setName(String name){ this.name = name;}

    public String getFilePath(){
        return this.filePath;
    }

    public void setFilePath(String filePath){
        this.filePath = filePath;
    }

    public List<Item> getItems(){
        return this.items;
    }




    public void displayList() {
            List<Item> items = this.items;
            System.out.println("Items:");
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                System.out.println((i + 1) + ". " + item.getName() + " (Quantity: " + item.getQuantity() + ")");

            }
            System.out.print("\n");
    }
    public void saveToFile() {
        // Implement saving the shopping list to a file
    }

    public void loadFromFile() {
        // Implement loading the shopping list from a file
    }
}
