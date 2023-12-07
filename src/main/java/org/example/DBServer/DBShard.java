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

    private static final int NUM_UNIQUE_SERVERS_BACKUP = 2;


    public DBShard(String shardFilePath, int port) {
        this.shardFilePath = shardFilePath;
        this.port = port;
        this.context = new ZContext();
        this.socket = context.createSocket(SocketType.ROUTER);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() {
        System.out.println("DBShard Server Running on Port: " + port);

        // Ping the server for hash ring info at startup
        pingServerForHashRing();

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
        Packet requestPacket = gson.fromJson(requestString, Packet.class);

        Packet responsePacket;
        try {
            ShoppingList incomingList;
            switch (requestPacket.getState()) {
                case LIST_UPDATE_REQUESTED_MAIN:
                    System.out.println("[LOG] Received MAIN write request");
                    requestPacket.setState(States.LIST_UPDATE_REQUESTED);
                    responsePacket = handleMainListUpdateRequest(requestPacket);
                    break;


                case LIST_UPDATE_REQUESTED:
                    System.out.println("[LOG] Received write request");
                    incomingList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
                    responsePacket = updateShoppingListOnServer(incomingList);
                    System.out.println("[LOG] Sent Response " + requestPacket.getState());
                    break;

                case RETRIEVE_LIST_REQUESTED_MAIN:
                    System.out.println("[LOG] Received MAIN write request");
                    String listId = requestPacket.getMessageBody();
                    //LOAD LIST
                    ShoppingList list = loadShoppingListWithId(listId);
                    requestPacket = new Packet(States.LIST_UPDATE_REQUESTED, gson.toJson(list, ShoppingList.class));
                    assert list != null;
                    responsePacket = handleMainListUpdateRequest(requestPacket);
                    ShoppingList fetchedList = gson.fromJson(responsePacket.getMessageBody(), ShoppingList.class);
                    responsePacket = new Packet(States.RETRIEVE_LIST_COMPLETED, gson.toJson(fetchedList));
                    break;
                case RETRIEVE_LIST_REQUESTED:
                    System.out.println("[LOG] Received read request");
                    ShoppingList requiredList = loadShoppingListWithId(requestPacket.getMessageBody());

                    if(requiredList == null){
                        responsePacket = new Packet(States.RETRIEVE_LIST_FAILED,"Request failed, list does not exist on server");
                    }else{
                        String listString = gson.toJson(requiredList);
                        responsePacket = new Packet(States.RETRIEVE_LIST_COMPLETED, listString );
                    }
                    System.out.println("[LOG] Sent list response");
                    break;
                case RETRIEVE_LISTS_REQUESTED:
                    List<ShoppingList> allLists = loadShoppingLists();
                    String allListsJson = gson.toJson(allLists);
                    responsePacket = new Packet(States.RETRIEVE_LISTS_COMPLETED, allListsJson);
                    break;

                case HASH_RING_UPDATE:
                    updateHashRing(requestPacket.getMessageBody());
                    responsePacket = new Packet(States.HASH_RING_UPDATE_ACK, "Hash ring updated successfully");

                    break;
                case PING:
                    System.out.println("[LOG] Received ping request");
                    responsePacket = new Packet(States.PONG, "Pong");
                    break;
                case PONG:
                    return;

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

        if (responsePacket.getState() == States.HASH_RING_UPDATE_ACK) {
            try{
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redistributeListsOnRingUpdate();
        }
    }

    private Packet handleMainListUpdateRequest(Packet requestPacket) throws IOException {
        ShoppingList updatedList = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
        Map<Integer, String> entry = hashRing.getServer(updatedList.getListId());
        int currentHash = entry.keySet().iterator().next();

        Set<String> forwardedServers = new HashSet<>();
        Set<String> triedServers = new HashSet<>();
        int numServersToForward = NUM_UNIQUE_SERVERS_BACKUP - 1; // Since main server is included
        int i = 0;

        while (forwardedServers.size() < numServersToForward) {
            Map<Integer, String> nextServerInfo = hashRing.getNextNthServer(currentHash, ++i);
            String nextServerAddress = nextServerInfo.values().iterator().next();

            if (nextServerAddress == null || nextServerAddress.equals("tcp://localhost:" + port) || forwardedServers.contains(nextServerAddress) || !triedServers.add(nextServerAddress)) {
                continue;
            }

            Packet forwardResponsePacket = sendToServer(nextServerAddress, requestPacket);
            if (forwardResponsePacket != null && forwardResponsePacket.getState() == States.LIST_UPDATE_COMPLETED) {
                // Merge the list from the next server with the list from this server
                ShoppingList responseList = gson.fromJson(forwardResponsePacket.getMessageBody(), ShoppingList.class);
                Packet finalPacket = updateShoppingListOnServer(responseList);
                gson.fromJson(finalPacket.getMessageBody(), ShoppingList.class).displayShoppingList();
                System.out.println("[LOG] final merged list above");
                forwardedServers.add(nextServerAddress);
            }
            else {
                System.out.println("[LOG] Failed to forward request to server: " + nextServerAddress);
            }
        }

        // Finally, merge the current list with the list from the user update
        System.out.println("[LOG] Merging lists");
        return updateShoppingList(updatedList);
    }


    private void updateHashRing(String hashRingData) {
        this.hashRing = gson.fromJson(hashRingData, HashRing.class);
        System.out.println("\n[LOG] Hash ring updated: \n" + hashRing.displayAllServers());
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
                return null;
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
                Integer targetHash = serverInfo.keySet().iterator().next();
                String targetServer = serverInfo.get(targetHash);

                Set<String> uniqueServers = new HashSet<>(serverInfo.values());

                int i = 1;
                while (uniqueServers.size() < NUM_UNIQUE_SERVERS_BACKUP && i < hashRing.getRing().size()) {
                    Map<Integer, String> nextServerInfo = hashRing.getNextNthServer(targetHash, i);
                    uniqueServers.addAll(nextServerInfo.values());
                    i++;
                }
                //System.out.println("[LOG] Unique servers: " + uniqueServers);
                if (uniqueServers.contains("tcp://localhost:" + port)) {
                    continue;  // Skip if the list should exist on this server
                }

                System.out.println("[LOG] Redistributing list " + list.getListId() + " to server " + targetServer);
                Packet updatePacket = new Packet(States.LIST_UPDATE_REQUESTED_MAIN, gson.toJson(list));

                sendToServer(targetServer, updatePacket);
                removeShoppingList(list.getListId().toString());
            }
        } catch (IOException e) {
            System.out.println("Error while redistributing lists: " + e.getMessage());
        }
    }

    private boolean pingServerForHashRing() {
        Packet requestPacket = new Packet(States.HASH_RING_UPDATE, "");
        String requestString = gson.toJson(requestPacket);

        // Send the request to the server
        try (ZMQ.Socket requestSocket = context.createSocket(SocketType.DEALER)) {
            requestSocket.connect("tcp://localhost:5555");
            requestSocket.send(requestString.getBytes(ZMQ.CHARSET), 0);
            requestSocket.setReceiveTimeOut(500);

            // Wait for a response
            byte[] responseBytes = requestSocket.recv(0);
            if (responseBytes != null) {
                String responseString = new String(responseBytes, ZMQ.CHARSET);
                Packet responsePacket = gson.fromJson(responseString, Packet.class);
                if (responsePacket.getState() == States.HASH_RING_UPDATE_ACK) {
                    updateHashRing(responsePacket.getMessageBody());
                    return true;
                }
                else {
                    System.out.println("Server offline, waiting for connection: ");
                }
            }
        } catch (Exception e) {
            System.out.println("Error pinging server for hash ring: " + e.getMessage());
        }
        return false;
    }




    private Packet updateShoppingList(ShoppingList updatedList) throws IOException {

        List<ShoppingList> existingLists = loadShoppingLists();
        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (ShoppingList existingList : existingLists) {
            if (existingList.getListId().equals(updatedList.getListId())) {
                updatedList= merge(existingList, updatedList); // new way of dealing with existing lists implementing CRDT merge function
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
            return packet;
        }
    }

    private Packet updateShoppingListOnServer(ShoppingList updatedList) throws IOException {
        List<ShoppingList> existingLists = loadShoppingLists();
        // Add or update the incoming list in the collection of existing lists
        boolean listExists = false;
        for (ShoppingList existingList : existingLists) {
            if (existingList.getListId().equals(updatedList.getListId())) {
                updatedList = merge(existingList, updatedList); // new way of dealing with existing lists implementing CRDT merge function
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
            return new Packet(States.LIST_UPDATE_COMPLETED, list);
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
            //System.out.println("[LOG] Saving updated shopping lists to file: " + shardFilePath);
            gson.toJson(lists, writer);
            writer.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Error saving updated shopping lists: " + e.getMessage());
            return false;
        }
    }

    public ShoppingList merge(ShoppingList List1, ShoppingList List2) {
        for (Map.Entry<String, CRDTItem> entry : List2.getItemList().entrySet()) {
            String itemNameItemFrom2 = entry.getKey();
            CRDTItem ItemFrom2 = entry.getValue();
                // Check if the item exists in the current list
                if (List1.getItemList().containsKey(itemNameItemFrom2)) {
                    CRDTItem ItemFrom1 = List1.getItemList().get(itemNameItemFrom2);
                    // Compare timestamps to determine the newer item in case of tie using user id
                    if (ItemFrom2.getTimestamp() > ItemFrom1.getTimestamp()
                            || ((ItemFrom2.getTimestamp() == ItemFrom1.getTimestamp())
                            && (ItemFrom2.getUserId().compareTo(ItemFrom1.getUserId()) > 0))) {
                        // Replace with the last change
                        List1.getItemList().put(itemNameItemFrom2, ItemFrom2); // otherwise replace the existing item on the list
                    }
                } else {
                    List1.getItemList().put(itemNameItemFrom2, ItemFrom2);
                }
        }
        System.out.println("[LOG] Merging lists");
        return List1;
    }

    public boolean removeShoppingList(String listId) {
        try {
            List<ShoppingList> existingLists = loadShoppingLists();
            Iterator<ShoppingList> iterator = existingLists.iterator();

            boolean listRemoved = false;
            while (iterator.hasNext()) {
                ShoppingList list = iterator.next();
                if (list.getListId().toString().equals(listId)) {
                    iterator.remove(); // Remove the matching list from the collection
                    listRemoved = true;
                    break;
                }
            }
            if (listRemoved) {
                if (saveUpdatedListsToFile(existingLists)) {
                    System.out.println("[LOG] Shopping list with ID " + listId + " removed successfully.");
                    return true;
                } else {
                    System.out.println("[ERROR] Failed to save updated shopping lists after removing list.");
                    return false;
                }
            } else {
                System.out.println("[LOG] Shopping list with ID " + listId + " not found on the server.");
                return false;
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Error removing shopping list: " + e.getMessage());
            return false;
        }
    }
}

