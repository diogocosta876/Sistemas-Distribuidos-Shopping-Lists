package org.example.DBServer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.CRDTItem;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.*;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        ShoppingList responseList;
        try {
            switch (requestPacket.getState()) {
                case LIST_UPDATE_REQUESTED:
                    ShoppingList updatedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    updatedList.displayShoppingList();
                    responseList = updateShoppingList(updatedList);
                    if(responseList == null){
                        responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Update failed, Could not save file on server");
                    }
                    else{
                        String list = gson.toJson(responseList);
                        responsePacket = new Packet(States.LIST_UPDATE_COMPLETED, list );                    }


                    break;
                case RETRIEVE_LIST_REQUESTED:
                    ShoppingList requiredList = loadShoppingListWithId(requestPacket.getMessageBody());

                    if(requiredList == null){
                        responsePacket = new Packet(States.RETRIEVE_LIST_FAILED,"Request failed, list does not exist on server");
                    }else{
                        String list = gson.toJson(requiredList);
                        responsePacket = new Packet(States.RETRIEVE_LIST_COMPLETED, list );
                    }
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
            responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Error processing request: " + e.getMessage()); //TODO this should not be this state
        }


        ZMsg responseMsg = new ZMsg();
        responseMsg.add(identityFrame);
        responseMsg.addString(gson.toJson(responsePacket));
        responseMsg.send(socket);
    }

    private ShoppingList updateShoppingList(ShoppingList updatedList) throws IOException {
        List<ShoppingList> existingLists = loadShoppingLists();
        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (ShoppingList existingList : existingLists) {
            if (existingList.getListId().equals(updatedList.getListId())) {

                merge(existingList, updatedList); // new way of dealing with existing lists implementing CRDT merge function
                updatedList = existingList;
                listExists = true;
                break;
            }
        }


        // list does not exist in server
        if (!listExists) {

            //add it into the server
            existingLists.add(updatedList);

        }

        if(!saveUpdatedListsToFile(existingLists)){
            return null;
        }else{
            return updatedList;
        }

    }

    private ShoppingList loadShoppingListWithId (String listId) throws IOException {

        List<ShoppingList> shoppingLists;
        File file = new File(shardFilePath);
        if (!file.exists()) {
            return null;
        }

        try (Reader reader = new FileReader(shardFilePath)) {
            Type listType = new TypeToken<ArrayList<ShoppingList>>(){}.getType();
            shoppingLists = gson.fromJson(reader, listType);
        } catch (FileNotFoundException e) {
            return null;
        }

        for (ShoppingList list : shoppingLists ) {
            if (list.getListId().toString().equals(listId)) {
                return list;
            }
        }

        return null;
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


    public void merge(ShoppingList existingList, ShoppingList incomingList) {
        for (Map.Entry<String, CRDTItem> entry : incomingList.getItemList().entrySet()) {

            String itemName = entry.getKey();
            CRDTItem incomingItem = entry.getValue();

            // Check if the item exists in the current list
            if (existingList.getItemList().containsKey(itemName)) {
                CRDTItem currentItem = existingList.getItemList().get(itemName);

                // Compare timestamps to determine the newer item in case of tie using user id
                if (incomingItem.getTimestamp() > currentItem.getTimestamp() || ((incomingItem.getTimestamp() == currentItem.getTimestamp())&& (incomingItem.getUserId().compareTo(currentItem.getUserId()) > 0))) {
                    // Replace with the newer item

                    if(incomingItem.getQuantity() == 0){ //item quantity == 0 means user deleted the item from the list
                        existingList.getItemList().remove(itemName);
                    }else{
                        existingList.getItemList().put(itemName, incomingItem); // otherwise replace the existing item on the list
                    }

                }
            } else {
                if(incomingItem.getQuantity()!=0){// this can occur when the user did not get the response from the server that the item was deleted
                    // Item doesn't exist in the current list, so we add it
                    existingList.getItemList().put(itemName, incomingItem);
                }

            }
        }

    }
}
