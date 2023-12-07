package org.example;

import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.CRDTItem;
import org.example.ShoppingList.ShoppingList;
import org.example.ShoppingList.ShoppingListManager;
import org.example.Client.User;
import org.jetbrains.annotations.NotNull;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.io.IOException;
import java.util.*;

public class RunClient {

    private User user;
    private ShoppingListManager listManager;

    List<String> UpdatedItemsList = new ArrayList<>();

    List<String> currentItems = new ArrayList<>();
    private ShoppingList selectedList;

    private final Scanner scanner;
    private boolean online;
    private final Gson gson = new Gson();

    private final ZMQ.Socket socket;
    private final ZContext context;
    private final ZMQ.Poller poller;

    public RunClient() {
        this.listManager = null;
        this.scanner = new Scanner(System.in);

        this.context = new ZContext(1);

        this.socket = context.createSocket(SocketType.DEALER);
        String serverAddress = "tcp://localhost:5555";
        this.socket.connect(serverAddress);

        this.poller = context.createPoller(1);
        poller.register(socket, ZMQ.Poller.POLLIN);
    }


    public void sendRequest(@NotNull Packet packet) {
        String serializedPacket = gson.toJson(packet);
        socket.send(serializedPacket.getBytes(ZMQ.CHARSET), 0);
    }

    public Packet receiveReply() {
        int rc = poller.poll(5000); // Set timeout
        if (rc == -1) {
            System.out.println("[LOG] Error polling socket.");
            return null;
        }
        if (poller.pollin(0)) {
            byte[] replyBytes = socket.recv(0);
            String replyString = new String(replyBytes, ZMQ.CHARSET);
            return gson.fromJson(replyString, Packet.class);
        } else {
            System.out.println("[LOG] No response from server.");
            return null;
        }
    }

    public boolean attemptHandshake(){
        System.out.println("[LOG] Pinging Server..");

        sendRequest(new Packet(States.HANDSHAKE_INITIATED, null));

        Packet reply = receiveReply();
        if (reply.getState() == States.HANDSHAKE_COMPLETED) {
            System.out.println("[LOG] Connection with server established.");
            return true;
        }

        return false;
    }

    public void run() throws IOException {
        this.user = new User();
        if (!user.authenticate()) return;

        this.listManager = new ShoppingListManager(user);

        while (true) {
            if(selectedList != null){
                System.out.println("Selected List: "+this.selectedList.getListName());
                this.selectedList.displayShoppingList();
            }
            System.out.println("1. Create a new shopping list");
            System.out.println("2. Select a shopping list");
            System.out.println("3. Add item to the selected list");
            System.out.println("4. Delete item from the selected list");
            System.out.println("5. Update item in the selected list");
            System.out.println("6. Delete current selected list");
            System.out.println("7. Import list");
            System.out.println("8. Exit");
            System.out.println("9. [TEMPORARY] Fetch all lists");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if(selectedList == null && (choice == 3 || choice == 4 || choice == 5 || choice == 6 )){
                System.out.println("No list selected. Please select a list first.");
            }else{
                switch (choice) {
                    case 1:
                        System.out.print("Enter the name of the shopping list: ");
                        String listName = scanner.nextLine();
                        this.listManager.createShoppingList(listName);
                        selectedList = this.listManager.getShoppingLists().get(this.listManager.getShoppingLists().size()-1);

                        break;
                    case 2:
                        if(!listManager.getShoppingLists().isEmpty())
                        {
                            selectShoppingList();
                        }else{
                            System.out.println("No list to show.");
                        }
                        break;
                    case 3:
                        addItemToSelectedList();
                        this.listManager.updateList(selectedList);
                        break;
                    case 4:
                        deleteItemFromSelectedList();
                        listManager.updateList(selectedList);
                        break;
                    case 5:
                        updateItemInSelectedList();
                        listManager.updateList(selectedList);
                        break;
                    case 6:
                        listManager.deleteShoppingList(selectedList.getListId());
                        UpdatedItemsList.clear();
                        selectedList = null;
                        break;
                    case 7:
                        importShoppingList();
                        break;
                    case 8:
                        System.out.println("Goodbye!");
                        return;
                    case 9:
                        System.out.println("Synchronizing lists...");
                        List<ShoppingList> lists = synchronizeShoppingLists();
                        for (ShoppingList list : lists) {
                            list.displayShoppingList();

                        }
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }

                if(choice == 1 || choice == 3 || choice == 4 || choice == 5){ //selected list changed so try to update list with server
                    selectedList.setState(org.example.ShoppingList.States.LOCAL_CHANGES);
                    synchronizeShoppingList(selectedList);
                    fillCurrentItems();
                    if(selectedList.getState()== org.example.ShoppingList.States.UPDATED){
                        System.out.println("Item changed:");
                        System.out.println(UpdatedItemsList);
                        UpdatedItemsList.clear();//clearing local updates tracker
                    }
                }
            }

        }
    }

    private void selectShoppingList() {
        this.listManager.displayShoppingLists();
        System.out.print("Enter the number of the list to select (0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice > 0 && choice <= listManager.getShoppingLists().size()) {
            selectedList = listManager.getShoppingLists().get(choice - 1);
            UpdatedItemsList.clear();// clearing track of current items because other list was selected
            fillCurrentItems();
        } else if (choice == 0) {
            System.out.println("Canceled selection.");
        } else {
            System.out.println("Invalid choice.");
        }
    }
    private void updateItemInSelectedList(){
        System.out.println("Name of the item to update: ");
        String itemName = scanner.nextLine();
        if(selectedList.getItemList().containsKey(itemName)){
            System.out.println("Quantity: ");
            int quantity = scanner.nextInt();
            if(selectedList.getItemList().get(itemName).getQuantity() == quantity){
                System.out.println("Same quantity this is not an update");
                return;
            }
            selectedList.getItemList().get(itemName).setQuantity(quantity);
            selectedList.getItemList().get(itemName).setUserId(user.uuid);
            if(!UpdatedItemsList.contains(itemName)){
                UpdatedItemsList.add(itemName);
                selectedList.getItemList().get(itemName).setTimestamp(selectedList.getItemList().get(itemName).getTimestamp()+1);

            }
        }else{
            System.out.println("No item found with that name");
            return;
        }
    }

    private void fillCurrentItems(){
        currentItems.clear();//clearing current items for next fill
        for(Map.Entry<String,CRDTItem> items : selectedList.getItemList().entrySet()){
            if(items.getValue().getQuantity()!= 0){
                currentItems.add(items.getKey());
            }
        }
    }

    private void addItemToSelectedList() {
        if (selectedList == null) {
            return;
        }

        System.out.print("Enter the item name: ");
        String itemName = scanner.nextLine();
        System.out.print("Enter the item quantity: ");
        int itemQuantity = scanner.nextInt();
        scanner.nextLine();
        if(selectedList.getItemList().containsKey(itemName)){
            System.out.println("Item already exists");
            return;
        }else{
            CRDTItem item = new CRDTItem(itemName,itemQuantity,0,user.uuid);
            UpdatedItemsList.add(itemName);
            selectedList.addItem(item);
            System.out.println("Item added to the selected list.");
        }
    }

    private void deleteItemFromSelectedList() {
        if (selectedList == null) {
            return;
        }

        selectedList.displayShoppingList();

        System.out.print("Enter the number of the item to delete (0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine();
        System.out.println("current items:"+currentItems);
        if (choice > 0 && choice <= currentItems.size()) {
            selectedList.removeItem(currentItems.get(choice-1));
            UpdatedItemsList.add(currentItems.get(choice-1));
            System.out.println("Item deleted from the selected list.");
        } else if (choice == 0) {
            System.out.println("Canceled deletion.");
        } else {
            System.out.println("Invalid choice.");
        }
    }



    public void synchronizeShoppingList(ShoppingList list) throws IOException {
        if (list == null) {
            System.out.println("No selected list to update.");
            return;
        }

        String listJson = gson.toJson(list);
        Packet request = new Packet(States.LIST_UPDATE_REQUESTED_MAIN, listJson);
        sendRequest(request);
        Packet reply = receiveReply();

        if (reply.getState() == States.LIST_UPDATE_COMPLETED) {
            System.out.println("[LOG] List updated successfully on the server.");
            selectedList = gson.fromJson(reply.getMessageBody(), ShoppingList.class);

            selectedList.setState(org.example.ShoppingList.States.UPDATED);
            listManager.updateList(selectedList);
            fillCurrentItems();

        } else if (reply.getState() == States.LIST_UPDATE_FAILED) {
            System.out.println("[LOG] Failed to update list on the server.");

        } else {
            System.out.println("Unexpected response from server.");
        }
    }

    public List<ShoppingList> synchronizeShoppingLists() throws IOException {
        //for now this method just fetches the lists from the server
        // TODO later it should compare the lists and update the server (CRDTS)
        sendRequest(new Packet(States.RETRIEVE_LISTS_REQUESTED, null));
        Packet reply = receiveReply();

        if (reply.getState() == States.RETRIEVE_LISTS_COMPLETED) {
            System.out.println("[LOG] Lists retrieved successfully from the server.");
            List<ShoppingList> lists = gson.fromJson(reply.getMessageBody(), List.class);

            //this.listManager.setShoppingLists(lists);
            return lists;
        } else {
            System.out.println("Unexpected response from server.");
        }
        return null;
    }

    public void importShoppingList(){
        System.out.println("Insert the shopping list Id to import: ");
        String listId = scanner.nextLine();

        sendRequest(new Packet(States.RETRIEVE_LIST_REQUESTED_MAIN, listId));
        Packet reply = receiveReply();

        if(reply.getState().equals(States.RETRIEVE_LIST_COMPLETED)){
            System.out.println("[LOG] List imported successfully.");
            System.out.println("Changing selected list to the imported list..");
            ShoppingList importedList = gson.fromJson(reply.getMessageBody(), ShoppingList.class);
            System.out.println("Imported list:");
            importedList.displayShoppingList();
            importedList.setState(org.example.ShoppingList.States.IMPORTED);
            importedList.setListName(importedList.getListName()+" / Imported");
            listManager.addShoppingList(importedList);
            selectedList = importedList;
            fillCurrentItems();
        }else{
            System.out.println("Unexpected response from server"); //TODO add messaging for client for him to know what happened
        }
    }

    private void setConnectionMode(boolean online) {
        this.online = online;
    }

    public static void main(String[] args) throws IOException {
        RunClient client = new RunClient();
        client.setConnectionMode(client.attemptHandshake());
        client.run();
    }

}
