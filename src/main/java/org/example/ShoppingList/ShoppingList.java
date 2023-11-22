package org.example.ShoppingList;

import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

public class ShoppingList implements Serializable {


    private final String listId;
    private final Map<String, CRDTItem> items;

    public ShoppingList(String listId) {
        this.listId = listId;
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
    public String getListId() {
        return listId;
    }

    // Merge two shopping lists
    public void merge(ShoppingList otherList) {
        for (Map.Entry<String, CRDTItem> entry : otherList.items.entrySet()) {
            items.computeIfAbsent(entry.getKey(), k -> new CRDTItem(entry.getKey())).merge(entry.getValue());
        }
    }
}
