package org.example.DBServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DBShard {
    private final String shardFilePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();;
    private final ZContext context;
    private int shardNumber;
    private final int port;
    private final ZMQ.Socket socket;

    public DBShard(String shardFilePath, int shardNumber, int port) {
        this.shardFilePath = shardFilePath;
        this.port = port;
        this.shardNumber = shardNumber;
        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.REP);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() {
        System.out.println("DBShard Server Running on Port: " + port);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                processRequest();
            } catch (IOException e) {
                System.out.println("Error processing request: " + e.getMessage());
            }
        }
    }

    public void processRequest() throws IOException {
        byte[] requestBytes = socket.recv(0);
        System.out.println("Received request: " + new String(requestBytes, ZMQ.CHARSET));
        String requestString = new String(requestBytes, ZMQ.CHARSET);
        Packet requestPacket = gson.fromJson(requestString, Packet.class);

        Packet responsePacket;
        try {
            switch (requestPacket.getState()) {
                case LIST_UPDATE_REQUESTED:
                    ShoppingList updatedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    responsePacket = updateShoppingList(updatedList) ?
                            new Packet(States.LIST_UPDATE_COMPLETED, "Update successful") :
                            new Packet(States.LIST_UPDATE_FAILED, "Update failed");
                    break;

                case LIST_DELETE_REQUESTED:
                    String listNameToDelete = gson.fromJson(requestPacket.getMessageBody(), String.class);
                    responsePacket = deleteShoppingList(listNameToDelete) ?
                            new Packet(States.LIST_DELETE_COMPLETED, "Deletion successful") :
                            new Packet(States.LIST_DELETE_FAILED, "Deletion failed");
                    break;

                default:
                    responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
            }
        } catch (Exception e) {
            responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Error processing request: " + e.getMessage());
        }

        String serializedResponse = gson.toJson(responsePacket);
        socket.send(serializedResponse.getBytes(ZMQ.CHARSET), 0);
    }

    private boolean updateShoppingList(ShoppingList updatedList) throws IOException {
        return saveShoppingLists(updatedList);
    }

    private boolean deleteShoppingList(String listName) throws IOException {
        List<ShoppingList> lists = loadShoppingLists();
        lists.removeIf(list -> list.getName().equals(listName));
        return saveUpdatedListsToFile(lists);
    }

    private boolean saveShoppingLists(ShoppingList incomingList) throws IOException {
        List<ShoppingList> existingLists = loadShoppingLists();

        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (int i = 0; i < existingLists.size(); i++) {
            if (existingLists.get(i).getName().equals(incomingList.getName())) {
                existingLists.set(i, incomingList);  // Update existing list
                listExists = true;
                break;
            }
        }

        if (!listExists) {
            existingLists.add(incomingList);  // Add new list if it doesn't exist
        }

        // Save the updated lists back to the file
        return saveUpdatedListsToFile(existingLists);
    }

    private List<ShoppingList> loadShoppingLists() throws IOException {
        File file = new File(shardFilePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(shardFilePath)) {
            Type listType = new TypeToken<ArrayList<ShoppingList>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
    }

    private boolean saveUpdatedListsToFile(List<ShoppingList> lists) throws IOException {
        try (Writer writer = new FileWriter(shardFilePath)) {
            System.out.println("Saving updated shopping lists to file: " + shardFilePath);
            gson.toJson(lists, writer);
            return true;
        } catch (IOException e) {
            System.out.println("Error saving updated shopping lists: " + e.getMessage());
            return false;
        }
    }
}
