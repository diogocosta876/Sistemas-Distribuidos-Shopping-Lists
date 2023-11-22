package org.example.ShoppingList;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class ShoppingList implements Serializable {


    private UUID uuid;
    private final Map<String, CRDTItem> items;

    private String listName;

    public ShoppingList(String listName) {
        this.uuid = java.util.UUID.randomUUID();;
        this.listName = listName;
        this.items = new HashMap<>();
    }

    // Add or update an item in the shopping list
    public void addItem(String itemName, int quantity, String userId, long timestamp) {
        CRDTItem item = items.computeIfAbsent(itemName, k -> new CRDTItem(itemName));
        item.updateItem(quantity, userId, timestamp);
    }

    // Get the current state of the shopping list
    public Map<String, CRDTItem> getState() {
        return new HashMap<>(items);
    }

    // Get the ID of the shopping list
    public UUID getUUID() {
        return uuid;
    }

    public String getListName(){
        return listName;
    }

    public void setName(String name){
        this.listName = name;
    }

    // Merge two shopping lists
    public void merge(ShoppingList otherList) {
        for (Map.Entry<String, CRDTItem> entry : otherList.items.entrySet()) {
            items.computeIfAbsent(entry.getKey(), k -> new CRDTItem(entry.getKey())).merge(entry.getValue());
        }
    }

    public void displayShoppingList() {
        System.out.println("Shopping List ID: " + getUUID());
        System.out.println("Items:");

        for (Map.Entry<String, CRDTItem> entry : getState().entrySet()) {
            CRDTItem item = entry.getValue();
            System.out.println("  Item: " + entry.getKey());
            System.out.println("  Quantity: " + item.getQuantity());
            System.out.println("    User ID: " + item.getUserId());
            System.out.println("    Timestamp: " + item.getTimeStamp());
            System.out.println("--------------");
        }
    }

}
