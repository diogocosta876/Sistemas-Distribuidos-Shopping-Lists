package org.example.ShoppingList;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoppingList implements Serializable {
    private final UUID listId;

    private final String listName;
    private Map<String, CRDTItem> itemList;

    private transient States state;



    public ShoppingList(String listName) {
        this.state = States.UNTRACKED;
        this.listId = java.util.UUID.randomUUID();
        this.listName = listName;
        this.itemList = new HashMap<>();
    }


    public UUID getListId() {
        return listId;
    }

    public String getListName() {return listName;}



    public Map<String, CRDTItem> getItemList(){return itemList;}

    public States getState(){return state;}


    public void setState(States newState){state = newState;}


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




    public void displayShoppingList() {
        System.out.println("Shopping List (" + listId + ")");
        System.out.println("STATE: " + state);

        if (itemList.isEmpty()) {
            System.out.println("The shopping list is empty.");
        } else {
            System.out.println("Items:");

            for (Map.Entry<String, CRDTItem> entry : itemList.entrySet()) {
                CRDTItem item = entry.getValue();

                if(item.getQuantity() != 0){// if item has quantity 0 it should not be displayed to the client, means the client deleted it and it has not yet synchronized with the server
                    System.out.println("  - " + item.getItemName() +
                            " | Quantity: " + item.getQuantity() +
                            " | Timestamp: " + item.getTimestamp());
                }else{
                    System.out.println("  - "+"Deleted " + item.getItemName() +
                            " | Quantity: " + item.getQuantity() +
                            " | Timestamp: " + item.getTimestamp());
                }
            }

        }
    }


}
