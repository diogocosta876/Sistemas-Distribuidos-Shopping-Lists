import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


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





    public ShoppingList loadShoppingListFromFile(String filePath) {
        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setFilePath(filePath);
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

    public String generateCustomID(String userId) {
        long currentTime = System.currentTimeMillis();

        int randomValue = (int) (Math.random() * 1000);
        return "user_" + userId+ "_"  + currentTime  + randomValue+ ".txt";
    }

    public List<ShoppingList> loadUserShoppingLists(String currentUserId) {
        String listsDirectory = "./ShoppingLists/lists/";
        List<ShoppingList> userShoppingLists = new ArrayList<>();

        File directory = new File(listsDirectory);
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();


                    if (fileName.contains("user_" + currentUserId)) {
                        // This file is associated with the current user
                        ShoppingList shoppingList = loadShoppingListFromFile(listsDirectory+fileName);
                        userShoppingLists.add(shoppingList);
                    }


                }
            }
        }



        return userShoppingLists.reversed();

    }
}
