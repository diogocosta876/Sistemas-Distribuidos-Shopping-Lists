package org.example;

import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.Item;
import org.example.ShoppingList.ShoppingList;
import org.example.ShoppingList.ShoppingListManager;
import org.example.Client.User;
import org.jetbrains.annotations.NotNull;
import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

public class RunClient {
    private ShoppingListManager listManager;
    private ShoppingList selectedList;
    private final Scanner scanner;
    private boolean online;
    private final Gson gson = new Gson();

    private final ZMQ.Socket socket;

    public RunClient() {
        this.listManager = null;
        this.scanner = new Scanner(System.in);
        ZContext context = new ZContext(1);
        this.socket = context.createSocket(SocketType.REQ);
        String serverAddress = "tcp://localhost:5555"; // this later needs to be setup dynamically
        this.socket.connect(serverAddress);
    }


    public void sendRequest(@NotNull Packet packet) {
        String serializedPacket = gson.toJson(packet);
        socket.send(serializedPacket.getBytes(ZMQ.CHARSET), 0);
    }

    public Packet receiveReply() {
        byte[] replyBytes = socket.recv(0);
        String replyString = new String(replyBytes, ZMQ.CHARSET);

        return gson.fromJson(replyString, Packet.class);
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
        User user = new User();
        if (!user.authenticate()) return;

        this.listManager = new ShoppingListManager(user);

        while (true) {
            if(selectedList != null){
                System.out.println("Selected List: "+this.selectedList.getName());
                this.selectedList.displayList();
            }

            System.out.println("1. Create a new shopping list");
            System.out.println("2. Select a shopping list");
            System.out.println("3. Add item to the selected list");
            System.out.println("4. Delete item from the selected list");
            System.out.println("5. Delete current selected list");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            if(selectedList == null && choice != 1 && choice != 2 && choice != 6){
                System.out.println("No list selected. Please select a list first.");
            }else{
                switch (choice) {
                    case 1:
                        System.out.print("Enter the name of the shopping list: ");
                        String listName = scanner.nextLine();
                        this.listManager.createShoppingList(listName);
                        selectedList = this.listManager.getShoppingLists().get(this.listManager.getShoppingLists().size()-1);
                        synchronizeShoppingList(selectedList);
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
                        synchronizeShoppingList(selectedList);
                        break;
                    case 4:
                        deleteItemFromSelectedList();
                        listManager.updateList(selectedList);
                        synchronizeShoppingList(selectedList);
                        break;
                    case 5:
                        listManager.deleteShoppingList(selectedList.getName());
                        synchronizeDeleteShoppingList(selectedList.getUUID());
                        selectedList = null;
                        break;
                    case 6:
                        System.out.println("Goodbye!");
                        return;
                    default:
                        System.out.println("Invalid choice. Please try again.");
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
        } else if (choice == 0) {
            System.out.println("Canceled selection.");
        } else {
            System.out.println("Invalid choice.");
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

        Item item = new Item(itemName, itemQuantity);
        selectedList.addItem(item);
        System.out.println("Item added to the selected list.");
    }

    private void deleteItemFromSelectedList() {
        if (selectedList == null) {
            return;
        }
        System.out.print("Enter the number of the item to delete (0 to cancel): ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        if (choice > 0 && choice <= selectedList.getItems().size()) {
            selectedList.removeItem(choice - 1);
            System.out.println("Item deleted from the selected list.");
        } else if (choice == 0) {
            System.out.println("Canceled deletion.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    public void synchronizeDeleteShoppingList(UUID listUuid) throws IOException {
        if (listUuid == null) {
            System.out.println("Invalid list identifier.");
            return;
        }
        String listUuidJson = gson.toJson(listUuid);

        sendRequest(new Packet(States.LIST_DELETE_REQUESTED, listUuidJson));
        Packet reply = receiveReply();

        if (reply.getState() == States.LIST_DELETE_COMPLETED) {
            System.out.println("List successfully deleted on the server.");
        } else if (reply.getState() == States.LIST_DELETE_FAILED) {
            System.out.println("Failed to delete list on the server.");
        } else {
            System.out.println("Unexpected response from server for delete request.");
        }
    }


    public void synchronizeShoppingList(ShoppingList list) throws IOException {
        if (list == null) {
            System.out.println("No selected list to update.");
            return;
        }
        String listJson = gson.toJson(list);
        sendRequest(new Packet(States.LIST_UPDATE_REQUESTED, listJson));
        Packet reply = receiveReply();

        if (reply.getState() == States.LIST_UPDATE_COMPLETED) {
            System.out.println("[LOG] List updated successfully on the server.");
        } else if (reply.getState() == States.LIST_UPDATE_FAILED) {
            System.out.println("[LOG] Failed to update list on the server.");
        } else {
            System.out.println("Unexpected response from server.");
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
