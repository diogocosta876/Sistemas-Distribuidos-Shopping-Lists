package org.example.Client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.ShoppingList.ShoppingList;
import org.example.ShoppingList.ShoppingListManager;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class User {

    public UUID uuid;
    private List<ShoppingList> lists;

    public User() {
        this.lists = new ArrayList<ShoppingList>();
    }

    public boolean authenticate() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        // Show user available options
        System.out.println("\nSelect an option for authentication:");
        System.out.println("\t0. Exit");
        System.out.println("\t1. Create new user");

        File folder = new File("./src/main/java/org/example/Client/UserData/");
        File[] listOfFiles = folder.listFiles();

        System.out.println("Available users:");
        // List available users
        for (int i = 0; i < Objects.requireNonNull(listOfFiles).length; i++) {
            System.out.println("\t" + (i + 2) + ". " + listOfFiles[i].getName().replace(".json", ""));
        }

        System.out.println("Choose an option: ");
        int option = Integer.parseInt(reader.readLine());

        // Option to exit
        if (option == 0) {
            System.out.println("Exiting...");
            return false;
        }

        // Option to create a new user
        if (option == 1) {
            this.uuid = UUID.randomUUID();
            saveToJson();
            System.out.println("Registration successful for user: " + this.uuid);
            return true;
        }

        // Existing user selection
        int userIndex = option - 2;
        if (userIndex < listOfFiles.length) {
            String userId = listOfFiles[userIndex].getName().replace(".json", "");
            User user = loadFromJson(userId);
            assert user != null;
            this.uuid = user.uuid;
            System.out.println("Authentication successful for user: " + this.uuid + "\n");

            return true;
        } else {
            System.out.println("Invalid option selected.");
            return false;
        }
    }

    public List<ShoppingList> getLists() {
        return this.lists;
    }

    public void setLists(List<ShoppingList> lists) {
        this.lists = lists;
        this.saveToJson();
    }

    public static User loadFromJson(String userId) {
        Gson gson = new Gson();
        String userFilePath = "./src/main/java/org/example/Client/UserData/" + userId + ".json";

        try (Reader reader = new FileReader(userFilePath)) {
            Type userType = new TypeToken<User>(){}.getType();
            return gson.fromJson(reader, userType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveToJson() {
        System.out.println("Saving user data...");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        String userFilePath = "./src/main/java/org/example/Client/UserData/" + this.uuid + ".json";

        try {
            Files.createDirectories(Paths.get(userFilePath).getParent());
            try (Writer writer = new FileWriter(userFilePath)) {
                writer.write(json);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}