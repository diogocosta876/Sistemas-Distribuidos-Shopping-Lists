package org.example.ShoppingList;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShoppingList implements Serializable {
    private final UUID listId;

    private String listName;
    private Map<String, CRDTItem> itemList;

    private transient States state;



    public ShoppingList(String listName) {
        this.state = States.UNTRACKED;
        this.listId = java.util.UUID.randomUUID();
        this.listName = listName;
        this.itemList = new HashMap<>();
    }

    public ShoppingList(String listName, UUID listId) {
        this.listId = listId;
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

    public void setListName(String newName){listName = newName;}


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
        CRDTItem item = getItemList().get(itemName);
        item.setTimestamp(item.getTimestamp()+1);//updating timestamp
        item.setQuantity(0);// setting quantity to 0 so merge function in server can handle it
    }




    public void displayShoppingList() {
        System.out.println("Shopping List (" + listId + ")");
        System.out.println("STATE: " + state);

        if (itemList.isEmpty()) {
            System.out.println("The shopping list is empty.");
        } else {
            System.out.println("Items:");
            int i = 1;
            for (Map.Entry<String, CRDTItem> entry : itemList.entrySet()) {
                CRDTItem item = entry.getValue();

                if(item.getQuantity() != 0){// if item has quantity 0 it should not be displayed to the client, means the client deleted it and it has not yet synchronized with the server
                    System.out.println(i+"."+"  - " + item.getItemName() +
                            " | Quantity: " + item.getQuantity() +
                            " | Timestamp: " + item.getTimestamp());
                    i++;
                }

            }

        }
    }

    public void displayShoppingListWithConflicts(Map<String, Integer> newConflicts) {
        System.out.println("Shopping List (" + listId + ")");
        System.out.println("STATE: " + state);

        System.out.println("Items:");
        int i = 1;
        for (Map.Entry<String, CRDTItem> entry : itemList.entrySet()) {
            CRDTItem item = entry.getValue();

            if(newConflicts.containsKey(item.getItemName())){// if item has quantity 0 it should not be displayed to the client, means the client deleted it and it has not yet synchronized with the serverc
                System.out.println(i+"."+"  - " + item.getItemName() +
                        " | Quantity chosen: " + item.getQuantity() +
                        " | Timestamp: " + item.getTimestamp() +
                        " | Quantity you wanted: " + newConflicts.get(item.getItemName()));
                newConflicts.remove(item.getItemName());
            } else {
                System.out.println(i+"."+"  - " + item.getItemName() +
                        " | Quantity: " + item.getQuantity() +
                        " | Timestamp: " + item.getTimestamp());
            }

            i++;
        }
        for (Map.Entry<String, Integer> entry : newConflicts.entrySet()) {
            System.out.println( " - " + entry.getKey() +
                    " was removed from the list, but you wanted quantity " + entry.getValue());
        }

        System.out.println("This list had conflicts, please review them.");
    }

}
