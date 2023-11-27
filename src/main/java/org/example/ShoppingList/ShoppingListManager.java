package org.example.ShoppingList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import org.example.Client.User;
import org.example.ShoppingList.ShoppingList;


public class ShoppingListManager {
    private List<ShoppingList> shoppingLists;
    private User user;
    private final String userDirectoryPath;

    public ShoppingListManager(User user){
        this.shoppingLists = new ArrayList<>();
        this.user = user;
        this.userDirectoryPath = "./src/main/java/org/example/Client/UserData/" + user.uuid + ".json";
        this.shoppingLists = user.getLists();
    }

    public void createShoppingList(String listName) {
        ShoppingList shoppingList = new ShoppingList(listName);

        shoppingLists.add(shoppingList);
        this.user.setLists(shoppingLists);
        System.out.println("Shopping list created: " + listName);
    }

    public void addShoppingList(ShoppingList shoppingList){
        for (ShoppingList list : shoppingLists) {
            if (list.getListName().equals(shoppingList.getListName())) {
                System.out.println("List with the same name already exists");
                return;
            }
        }
        shoppingLists.add(shoppingList);
    }

    public void deleteShoppingList(String listName) {
        // Find and remove the shopping list from the in-memory list
        boolean isRemoved = shoppingLists.removeIf(list -> list.getListName().equals(listName));

        if (isRemoved) {
            // If a list was removed, update the user's lists and save to JSON
            user.setLists(shoppingLists);
            System.out.println("Successfully deleted shopping list: " + listName);
        } else {
            System.err.println("Shopping list not found: " + listName);
        }
    }

    public List<ShoppingList> getShoppingLists() {
        return shoppingLists;
    }

    public void setShoppingLists(List<ShoppingList> shoppingLists){
        this.user.setLists(shoppingLists);
    }

    public void displayShoppingLists() {
        System.out.println("Shopping Lists:");
        List<ShoppingList> lists = this.shoppingLists;
        for (int i = 0; i < lists.size(); i++) {
            System.out.println((i + 1) + ". " + lists.get(i).getListName());
        }
    }

    public void saveShoppingList(ShoppingList shoppingList) {
        for (int i = 0; i < user.getLists().size(); i++) {
            System.out.println(this.shoppingLists.get(i).getListName());
        }
        System.out.println("after");
        this.shoppingLists.add(shoppingList);
        user.setLists(this.shoppingLists);
        for (int i = 0; i < user.getLists().size(); i++) {
            System.out.println(this.shoppingLists.get(i).getListName());
        }
        user.saveToJson();
    }


    public void updateList(ShoppingList selectedList) {
        if (selectedList != null && selectedList.getListName() != null) {
            // Iterate over the shoppingLists to find the matching list by name
            for (int i = 0; i < shoppingLists.size(); i++) {
                if (shoppingLists.get(i).getListName().equals(selectedList.getListName())) {
                    //print items from selected list
                    shoppingLists.get(i).displayShoppingList();
                    shoppingLists.set(i, selectedList);
                    System.out.println("List updated: " + selectedList.getListName());
                }
            }
        } else {
            System.err.println("The provided shopping list is null or has no name.");
        }
        user.setLists(shoppingLists);
    }
}
