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
    private String userDirectoryPath;

    public ShoppingListManager(User user){
        this.shoppingLists = new ArrayList<>();
        this.user = user;
        this.userDirectoryPath = "./src/main/java/org/example/Client/UserData/" + user.userId + ".json";
        this.shoppingLists = user.getLists();
    }

    public void createShoppingList(String listName) {
        ShoppingList shoppingList = new ShoppingList(listName);
        String listId = generateCustomID(user.userId);
        shoppingLists.add(shoppingList);
        this.user.setLists(shoppingLists);
        System.out.println("Shopping list created: " + listName);
    }

    public void addShoppingList(ShoppingList shoppingList){
        //TODO prevent creation from shopping list with the same name
        shoppingLists.add(shoppingList);
    }

    public void deleteShoppingList(String listName) {
        // Find and remove the shopping list from the in-memory list
        boolean isRemoved = shoppingLists.removeIf(list -> list.getName().equals(listName));

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

    public void setShoppingLists(){
        this.user.setLists(shoppingLists);
    }

    public void displayShoppingLists() {
        System.out.println("Shopping Lists:");
        List<ShoppingList> lists = this.shoppingLists;
        for (int i = 0; i < lists.size(); i++) {
            System.out.println((i + 1) + ". " + lists.get(i).getName());
        }
    }

    public void saveShoppingList(ShoppingList shoppingList) {
        for (int i = 0; i < user.getLists().size(); i++) {
            System.out.println(this.shoppingLists.get(i).getName());
        }
        System.out.println("after");
        this.shoppingLists.add(shoppingList);
        user.setLists(this.shoppingLists);
        for (int i = 0; i < user.getLists().size(); i++) {
            System.out.println(this.shoppingLists.get(i).getName());
        }
        user.saveToJson();
    }


    public ShoppingList loadShoppingListFromJson(String filePath) {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, ShoppingList.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String generateCustomID(String userId) {

        long seed = System.currentTimeMillis();
        Random random = new Random(seed);


        return "user_" + userId+ "_"  + Math.abs(random.nextInt())  + Math.abs(random.nextInt())+ ".txt";
    }

    public List<ShoppingList> loadUserShoppingLists(String currentUserId) {
        String listsDirectory = "./lists/";
        List<ShoppingList> userShoppingLists = new ArrayList<>();

        File directory = new File(listsDirectory);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();


                    if (fileName.contains("user_" + currentUserId)) {
                        ShoppingList shoppingList = loadShoppingListFromJson(listsDirectory+fileName);
                        userShoppingLists.add(shoppingList);
                    }


                }
            }
        }



        return userShoppingLists.reversed();

    }

    public void updateList(ShoppingList selectedList) {
        if (selectedList != null && selectedList.getName() != null) {
            // Iterate over the shoppingLists to find the matching list by name
            for (int i = 0; i < shoppingLists.size(); i++) {
                if (shoppingLists.get(i).getName().equals(selectedList.getName())) {
                    //print items from selected list
                    shoppingLists.get(i).displayList();
                    shoppingLists.set(i, selectedList);
                    System.out.println("List updated: " + selectedList.getName());
                }
            }
        } else {
            System.err.println("The provided shopping list is null or has no name.");
        }
        user.setLists(shoppingLists);
    }
}
