package org.example.ShoppingList;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoppingList implements Serializable {
    private final UUID listId;

    private final String listName;
    private long timestamp;
    private Map<String, CRDTItem> itemList;

    public ShoppingList(String listName) {
        this.listId = java.util.UUID.randomUUID();
        this.listName = listName;
        this.timestamp = 0;
        this.itemList = new HashMap<>();
    }


    public UUID getListId() {
        return listId;
    }

    public String getListName() {return listName;}

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, CRDTItem> getItemList(){return itemList;}


    public void addItem(CRDTItem newItem) {
        String itemName = newItem.getItemName();
        if (!itemList.containsKey(itemName)) {
            itemList.put(itemName, newItem);
        } else {
            CRDTItem existingItem = itemList.get(itemName);

            // Update the existing item's quantity and timestamp
            existingItem.setQuantity(newItem.getQuantity());
            existingItem.setTimestamp(existingItem.getTimestamp() + 1);



            System.out.println("Item updated: " + existingItem.getItemName());
        }


    }


    // Method to remove an item from the shopping list
    public void removeItem(String itemName) {
        itemList.remove(itemName);
    }

    // Merge function to merge two shopping lists pass it to server maybe
    public void merge(ShoppingList otherList) {
        for (Map.Entry<String, CRDTItem> entry : otherList.itemList.entrySet()) {
            String itemName = entry.getKey();
            CRDTItem otherItem = entry.getValue();

            // Check if the item exists in the current list
            if (itemList.containsKey(itemName)) {
                CRDTItem currentItem = itemList.get(itemName);

                // Compare timestamps to determine the newer item
                if (otherItem.getTimestamp() > currentItem.getTimestamp()) {
                    // Replace with the newer item
                    itemList.put(itemName, otherItem);
                }
            } else {
                // Item doesn't exist in the current list, add it
                itemList.put(itemName, otherItem);
            }
        }

        // Update the timestamp after the merge
        timestamp = Math.max(timestamp, otherList.getTimestamp());
    }


    public void displayShoppingList() {
        System.out.println("Shopping List (" + listId + ")");
        System.out.println("Timestamp: " + timestamp);

        if (itemList.isEmpty()) {
            System.out.println("The shopping list is empty.");
        } else {
            System.out.println("Items:");

            for (Map.Entry<String, CRDTItem> entry : itemList.entrySet()) {
                CRDTItem item = entry.getValue();
                System.out.println("  - " + item.getItemName() +
                        " | Quantity: " + item.getQuantity() +
                        " | Timestamp: " + item.getTimestamp());
            }
        }
    }

    // Serialization method
    /*
    public String serialize() {
        StringBuilder json = new StringBuilder("{\"listId\":\"" + listId + "\",\"timestamp\":" + timestamp + ",\"itemList\":{");

        for (Map.Entry<String, CRDTItem> entry : itemList.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().serialize()).append(",");
        }

        if (!itemList.isEmpty()) {
            json.deleteCharAt(json.length() - 1); // Remove the trailing comma
        }

        json.append("}}");

        return json.toString();
    }

     */
}
