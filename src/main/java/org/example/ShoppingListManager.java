package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import com.google.gson.Gson;


public class ShoppingListManager {
    private List<ShoppingList> shoppingLists;

    public ShoppingListManager() {
        this.shoppingLists = new ArrayList<>();

    }

    public void createShoppingList(String listName,String userId) {
        ShoppingList shoppingList = new ShoppingList(listName);
        String listId = generateCustomID(userId);
        shoppingList.setFilePath(shoppingList.getFilePath()+listId);
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
                File file = new File(lists.get(i).getFilePath());

                if (file.exists() && file.isFile()) {
                    if (file.delete()) {
                        System.out.println("File deleted successfully.");
                    } else {
                        System.err.println("Failed to delete the file.");
                    }
                } else {
                    System.err.println("The specified file does not exist or is not a regular file.");
                }
                lists.remove(i);
                break;

            }
        }


    }

    public List<ShoppingList> getShoppingLists() {
        return shoppingLists;
    }

    public void setShoppingLists(List<ShoppingList> shoppingLists){
        this.shoppingLists = shoppingLists;
    }

    public void displayShoppingLists() {
        System.out.println("Shopping Lists:");
        List<ShoppingList> lists = this.shoppingLists;
        for (int i = 0; i < lists.size(); i++) {
            System.out.println((i + 1) + ". " + lists.get(i).getName());
        }
    }

    public void saveShoppingListToJson(ShoppingList shoppingList, String filePath) {
        Gson gson = new Gson();
        String json = gson.toJson(shoppingList);

        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.write(json);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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
}
