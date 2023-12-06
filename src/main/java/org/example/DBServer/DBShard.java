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
import java.util.*;

public class DBShard {
    private final String shardFilePath;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final ZContext context;
    private final int port;
    private final ZMQ.Socket socket;
    private HashRing hashRing;


    public DBShard(String shardFilePath, int port) {
        this.shardFilePath = shardFilePath;
        this.port = port;
        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.ROUTER);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() {
        System.out.println("DBShard Server Running on Port: " + port);
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg msg = ZMsg.recvMsg(socket);
            if (msg != null) {
                processRequest(msg);
            }
        }
    }

    private void processRequest(ZMsg msg)  {
        ZFrame identityFrame = msg.pop();
        ZFrame contentFrame = msg.pop();

        String requestString = new String(contentFrame.getData(), ZMQ.CHARSET);

        if ("ping".equals(requestString)) {
            ZMsg responseMsg = new ZMsg();
            responseMsg.add(identityFrame);
            responseMsg.addString("pong");
            System.out.println("connected to server");
            responseMsg.send(socket);
            return;
        }

        Packet requestPacket = gson.fromJson(requestString, Packet.class);


        Packet responsePacket;
        try {
            ShoppingList updatedList;
            switch (requestPacket.getState()) {
                case LIST_UPDATE_REQUESTED_MAIN:
                    System.out.println("[LOG] Received MAIN write request");
                    //forward to next server
                    requestPacket.setState(States.LIST_UPDATE_REQUESTED);
                    updatedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    Map<Integer, String> entry = hashRing.getServer(updatedList.getListId());
                    int hash = entry.keySet().iterator().next();
                    Packet forwardResponsePacket = forwardRequestToNextServer(requestPacket, hash);
                    assert forwardResponsePacket != null;

                    //merge the list from next server with the list from this server
                    Map<String,Integer> extraInfo = forwardResponsePacket.getExtraInfo();
                    ShoppingList responseList = gson.fromJson(forwardResponsePacket.getMessageBody(), ShoppingList.class);
                    updateShoppingListOnServer(responseList, extraInfo);

                    //merge the current list with the list from the user update
                    System.out.println("[LOG] Merging lists");
                    responsePacket = updateShoppingList(updatedList, requestPacket.getExtraInfo());
                    break;

                case LIST_UPDATE_REQUESTED:
                    updatedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    updatedList.displayShoppingList();
                    responsePacket = updateShoppingListOnServer(updatedList,requestPacket.getExtraInfo());
                    break;

                case RETRIEVE_LIST_REQUESTED_MAIN:
                    System.out.println("[LOG] Received MAIN read request");
                    //forward to next server
                    requestPacket.setState(States.RETRIEVE_LIST_REQUESTED);
                    ShoppingList requestedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    entry = hashRing.getServer(requestedList.getListId());
                    hash = entry.keySet().iterator().next();
                    forwardResponsePacket = forwardRequestToNextServer(requestPacket, hash);
                    assert forwardResponsePacket != null;
                    extraInfo = forwardResponsePacket.getExtraInfo();

                    //merge the list from next server with the list from this server
                    if (forwardResponsePacket.getState() == States.RETRIEVE_LIST_FAILED) {
                        responsePacket = new Packet(States.RETRIEVE_LIST_FAILED,"Request failed, list does not exist on next server");
                        ShoppingList requiredList = loadShoppingListWithId(requestPacket.getMessageBody());

                        if(requiredList == null){
                            responsePacket = new Packet(States.RETRIEVE_LIST_FAILED,"Request failed, list does not exist on server");
                        }else{ //list exists locally
                            String list = gson.toJson(requiredList);
                            responsePacket = new Packet(States.RETRIEVE_LIST_COMPLETED, list );
                        }
                        break;
                    }
                    else { //list exists on next server
                        responseList = gson.fromJson(forwardResponsePacket.getMessageBody(), ShoppingList.class);
                        updateShoppingListOnServer(responseList, extraInfo);
                        //merge the current list with the list from the user update
                        ShoppingList requiredList = loadShoppingListWithId(requestPacket.getMessageBody());
                        merge(requiredList, responseList, extraInfo);

                        if(requiredList == null){
                            responsePacket = new Packet(States.RETRIEVE_LIST_FAILED,"Request failed, list does not exist on server");
                        }else{ //list exists locally
                            String list = gson.toJson(requiredList);
                            responsePacket = new Packet(States.RETRIEVE_LIST_COMPLETED, list );
                        }
                        break;
                    }
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

                case HASH_RING_UPDATE:
                    updateHashRing(requestPacket.getMessageBody());
                    responsePacket = new Packet(States.HASH_RING_UPDATE_ACK, "Hash ring updated successfully");
                    //timeout to give time for all servers to update their hash rings
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    redistributeListsOnRingUpdate();
                    break;

                default:
                    responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
            }
        } catch (Exception e) {
            responsePacket = new Packet(States.LIST_UPDATE_FAILED, "Error processing request: " + e.getMessage());
            System.out.println("Error processing request: " + e.getMessage());
        }


        ZMsg responseMsg = new ZMsg();
        responseMsg.add(identityFrame);
        responseMsg.addString(gson.toJson(responsePacket));
        responseMsg.send(socket);
        System.out.println("\n");
    }

    private void updateHashRing(String hashRingData) {
        this.hashRing = gson.fromJson(hashRingData, HashRing.class);
        System.out.println("\n[LOG] Hash ring updated: \n" + hashRing.displayAllServers());
    }

    private Packet forwardRequestToNextServer(Packet requestPacket, int currentServerHash) {
        Map<Integer, String> nextServerInfo = hashRing.getNextNthServer(currentServerHash, 1);
        Map.Entry<Integer, String> nextServerEntry = nextServerInfo.entrySet().iterator().next();
        String nextServerAddress = nextServerEntry.getValue();

        if (!nextServerAddress.equals("tcp://localhost:" + port)) {
            return sendToServer(nextServerAddress, requestPacket);
        }
        else {
            nextServerInfo = hashRing.getNextNthServer(currentServerHash, 2);
            nextServerEntry = nextServerInfo.entrySet().iterator().next();
            nextServerAddress = nextServerEntry.getValue();
            if (!nextServerAddress.equals("tcp://localhost:" + port)) {
                return sendToServer(nextServerAddress, requestPacket);
            }
        }
        return null;
    }

    private Packet sendToServer(String serverAddress, Packet requestPacket) {
        try (ZMQ.Socket forwardSocket = context.createSocket(SocketType.DEALER)) {
            String requestString = gson.toJson(requestPacket);
            forwardSocket.connect(serverAddress);
            forwardSocket.send(requestString.getBytes(ZMQ.CHARSET), 0);
            System.out.println("[LOG] Request forwarded to server: " + serverAddress);

            forwardSocket.setReceiveTimeOut(1000);

            byte[] responseBytes = forwardSocket.recv(0);
            if (responseBytes != null) {
                String responseString = new String(responseBytes, ZMQ.CHARSET);
                System.out.println("[LOG] Received response from server: " + serverAddress);
                return gson.fromJson(responseString, Packet.class);
            } else {
                System.out.println("No response received within the timeout period.");
            }
        } catch (Exception e) {
            System.out.println("Error forwarding request to server " + serverAddress + ": " + e.getMessage());
        }
        return null;
    }

    private void redistributeListsOnRingUpdate() {
        try {
            List<ShoppingList> allLists = loadShoppingLists();
            for (ShoppingList list : allLists) {
                Map<Integer, String> serverInfo = hashRing.getServer(list.getListId());
                Integer hash = serverInfo.keySet().iterator().next();
                String targetServer = serverInfo.get(hash);

                if (!targetServer.equals("tcp://localhost:" + port)) {
                    Map<String, Integer> itemMap = new HashMap<>();
                    for (Map.Entry<String, CRDTItem> entry : list.getItemList().entrySet()) {
                        itemMap.put(entry.getKey(), 0);
                    }
                    System.out.println("[LOG] Redistributing list " + list.getListId() + " to server " + targetServer);
                    Packet updatePacket = new Packet(States.LIST_UPDATE_REQUESTED_MAIN, gson.toJson(list));
                    updatePacket.setExtraInfo(itemMap);
                    sendToServer(targetServer, updatePacket);
                }
            }
        } catch (IOException e) {
            System.out.println("Error while redistributing lists: " + e.getMessage());
        }
    }



    private ShoppingList withoutDeleted(ShoppingList existing) {
        ShoppingList list = new ShoppingList(existing.getListName(),existing.getListId());
        for (Map.Entry<String, CRDTItem> entry : existing.getItemList().entrySet()) {
            if (entry.getValue().getQuantity() != 0) {
                list.addItem(entry.getValue());
            }
        }
        return list;
    }
    private Packet updateShoppingList(ShoppingList updatedList,Map<String,Integer> extraInfo) throws IOException {

        Map<String,Integer> conflicts = new HashMap<>();

        List<ShoppingList> existingLists = loadShoppingLists();
        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (ShoppingList existingList : existingLists) {
            if (existingList.getListId().equals(updatedList.getListId())) {
                conflicts = merge(existingList, updatedList,extraInfo); // new way of dealing with existing lists implementing CRDT merge function
                updatedList = withoutDeleted(existingList);
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
            return new Packet(States.LIST_UPDATE_FAILED, "Update failed, Could not save file on server");
        }else{
            String list = gson.toJson(updatedList);
            Packet packet = new Packet(States.LIST_UPDATE_COMPLETED, list );
            packet.setExtraInfo(conflicts);
            return packet;
        }
    }

    private Packet updateShoppingListOnServer(ShoppingList updatedList,Map<String,Integer> extraInfo) throws IOException {
        List<ShoppingList> existingLists = loadShoppingLists();
        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (ShoppingList existingList : existingLists) {
            if (existingList.getListId().equals(updatedList.getListId())) {
                merge(existingList, updatedList,extraInfo); // new way of dealing with existing lists implementing CRDT merge function
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
            return new Packet(States.LIST_UPDATE_FAILED, "Update failed, Could not save file on server");
        }else{
            String list = gson.toJson(updatedList);
            Packet packet = new Packet(States.LIST_UPDATE_COMPLETED, list);
            packet.setExtraInfo(extraInfo);
            return packet;
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

    private boolean saveUpdatedListsToFile(List<ShoppingList> lists) {
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

    public Map<String,Integer> merge(ShoppingList existingList, ShoppingList incomingList,Map<String,Integer> itemsUpdated) {
        Map<String,Integer> conflicts = new HashMap<>();
        for (Map.Entry<String, CRDTItem> entry : incomingList.getItemList().entrySet()) {

            String itemName = entry.getKey();
            CRDTItem incomingItem = entry.getValue();
            if(itemsUpdated.containsKey(itemName)){
                // Check if the item exists in the current list
                if (existingList.getItemList().containsKey(itemName)) {
                    CRDTItem currentItem = existingList.getItemList().get(itemName);
                    if(currentItem.getQuantity() == 0){ // means the item was deleted my an user and it is being pushed again so we need to add it and set the timestamp to 0 on the server
                        existingList.getItemList().put(itemName,incomingItem);
                    }else{
                        // Compare timestamps to determine the newer item in case of tie using user id
                        if (incomingItem.getTimestamp() > currentItem.getTimestamp()
                                || ((incomingItem.getTimestamp() == currentItem.getTimestamp())
                                && (incomingItem.getUserId().compareTo(currentItem.getUserId()) > 0))) {
                            // Replace with the last change
                            existingList.getItemList().put(itemName, incomingItem); // otherwise replace the existing item on the list
                        } else {
                            conflicts.put(incomingItem.getItemName(),incomingItem.getQuantity());
                        }
                    }
                } else {
                    incomingItem.setTimestamp(0);// if an item was deleted my other user and is again pushed it should have timestamp 0 not 1
                    existingList.getItemList().put(itemName, incomingItem);
                }
            }else{
                if(existingList.getItemList().get(incomingItem.getItemName()).getQuantity() == 0){
                    conflicts.put(incomingItem.getItemName(),incomingItem.getQuantity());//if we catch this conflict is a special case and the show function on the client should behave differently
                }
            }
        }
        //System.out.println("[LOG] Num Conflicts found: "+conflicts.size());
        return conflicts;
    }
}
