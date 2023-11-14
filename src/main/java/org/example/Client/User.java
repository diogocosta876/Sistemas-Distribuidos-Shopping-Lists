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

public class User {

    public String userId; //change to private later and implement getters
    public String password; // Passwords should be hashed in production!
    private List<ShoppingList> lists;

    public User() {
        this.lists = new ArrayList<ShoppingList>();
    }

    public User(String userId, String password) {
        this.userId = userId;
        this.password = password;
        this.lists = new ArrayList<ShoppingList>();
    }

    public boolean authenticate() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Enter your unique username: ");
        String userId = reader.readLine();
        String userFilePath = "./src/main/java/org/example/Client/UserData/" + userId + ".json";

        if (Files.exists(Paths.get(userFilePath))) {
            // User JSON exists, so try to log in
            User user = loadFromJson(userId);
            if (user == null) {
                System.out.println("Failed to load user data.");
                return false;
            }

            this.userId = user.userId;
            this.password = user.password;
            this.lists = user.lists;
            System.out.println("Session refreshed for user: " + user.userId);
            return true;
        } else {
            // User JSON does not exist, so register a new user
            System.out.println("No such user found. Let's register you.");
            System.out.println("Input your password: ");
            String password = reader.readLine();

            this.userId = userId;
            this.password = password; // In production, hash the password
            saveToJson();
            System.out.println("Registration successful.");
            return true;
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
        String userFilePath = "./src/main/java/org/example/Client/UserData/" + this.userId + ".json";

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