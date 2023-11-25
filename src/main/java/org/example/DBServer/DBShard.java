package org.example.DBServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.*;

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
        this.socket = context.createSocket(SocketType.ROUTER);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() throws IOException {
        System.out.println("DBShard Server Running on Port: " + port);
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg msg = ZMsg.recvMsg(socket);
            if (msg != null) {
                processRequest(msg);
            }
        }
    }

    private void processRequest(ZMsg msg) throws IOException {
        ZFrame identityFrame = msg.pop();
        ZFrame contentFrame = msg.pop();

        String requestString = new String(contentFrame.getData(), ZMQ.CHARSET);
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

                case RETRIEVE_LISTS_REQUESTED:
                    List<ShoppingList> allLists = loadShoppingLists();
                    String allListsJson = gson.toJson(allLists);
                    responsePacket = new Packet(States.RETRIEVE_LISTS_COMPLETED, allListsJson);
                    break;

                default:
                    responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
            }
        } catch (Exception e) {
            responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Error processing request: " + e.getMessage());
        }

        String serializedResponse = gson.toJson(responsePacket);
        ZMsg responseMsg = new ZMsg();
        responseMsg.add(identityFrame);
        responseMsg.addString(gson.toJson(responsePacket));
        responseMsg.send(socket);
    }

    private boolean updateShoppingList(ShoppingList updatedList) throws IOException {
        List<ShoppingList> existingLists = loadShoppingLists();

        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (int i = 0; i < existingLists.size(); i++) {
            if (existingLists.get(i).getListName().equals(updatedList.getListName())) {
                existingLists.set(i, updatedList);
                listExists = true;
                break;
            }
        }

        if (!listExists) {
            existingLists.add(updatedList);
        }

        return saveUpdatedListsToFile(existingLists);
    }

    private boolean deleteShoppingList(String listName) throws IOException {
        List<ShoppingList> lists = loadShoppingLists();
        lists.removeIf(list -> list.getListName().equals(listName));
        return saveUpdatedListsToFile(lists);
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
            writer.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Error saving updated shopping lists: " + e.getMessage());
            return false;
        }
    }
}
