package org.example;
import com.google.gson.Gson;
import org.example.DBServer.HashRing;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RunRouterServer {
    private final ZMQ.Socket clientSocket;
    private final ZContext context = new ZContext(1);
    private final Gson gson;
    private final HashRing hashRing;

    public RunRouterServer(int port) {
        gson = new Gson();

        this.clientSocket = context.createSocket(SocketType.ROUTER);
        this.clientSocket.bind("tcp://*:" + port);

        this.hashRing = new HashRing();

        //TODO check which servers are running and send them the hash ring
        discoverAndConnectToServers();

        //DEBUG
        System.out.println("\nHash ring:");
        System.out.println(hashRing.displayAllServers());
    }

    public void run() {
        System.out.println("Server Running");
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg msg = ZMsg.recvMsg(clientSocket);
            ZFrame identityFrame = msg.pop();
            ZFrame contentFrame = msg.pop();

            String requestString = new String(contentFrame.getData(), ZMQ.CHARSET);
            Packet requestPacket = gson.fromJson(requestString, Packet.class);

            System.out.println("[LOG] Received request: " + requestPacket);

            Packet responsePacket = processRequest(requestPacket);

            String serializedResponse = gson.toJson(responsePacket);

            ZMsg replyMsg = new ZMsg();
            replyMsg.add(identityFrame);
            replyMsg.addString(serializedResponse);
            replyMsg.send(clientSocket);

            System.out.println("[LOG] Sent Client response: " + responsePacket);
        }
    }

    private Packet processRequest(Packet requestPacket) {
        switch (requestPacket.getState()) {
            case HANDSHAKE_INITIATED:
                System.out.println("[LOG] Handshake initiated by client.");
                return new Packet(States.HANDSHAKE_COMPLETED, "[LOG] Handshake successful");

            case LIST_UPDATE_REQUESTED_MAIN:
            case LIST_DELETE_REQUESTED:
            case RETRIEVE_LIST_REQUESTED:
                return forwardRequestToDBServer(requestPacket);

                //TODO implement RETRIEVE ALL LISTS request
            default:
                System.out.println("Invalid request state: " + requestPacket.getState());
                return new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
        }
    }

    private Packet forwardRequestToDBServer(Packet requestPacket) {
        String requestString = gson.toJson(requestPacket);
        String listID = getListIDFromPacket(requestPacket);

        Map<Integer, String> serverInfo = hashRing.getServer(listID);
        int attempt = 0;
        int maxAttempts = 3;

        while (attempt < maxAttempts) {
            Map.Entry<Integer, String> entry = serverInfo.entrySet().iterator().next();
            Integer hash = entry.getKey();
            String serverIP = entry.getValue();
            System.out.println("[LOG] Attempting to forward request to DB server: " + serverIP);

            try (ZMQ.Socket dbSocket = context.createSocket(SocketType.DEALER)) {
                dbSocket.connect(serverIP);
                dbSocket.send(requestString.getBytes(ZMQ.CHARSET), 0);

                ZMQ.Poller poller = context.createPoller(1);
                poller.register(dbSocket, ZMQ.Poller.POLLIN);
                boolean hasReply = poller.poll(1000) > 0; // 300 milliseconds timeout

                if (hasReply) {
                    String responseString = dbSocket.recvStr();
                    System.out.println("[LOG] Received response from DB server: " + responseString);
                    return gson.fromJson(responseString, Packet.class);
                } else {
                    System.out.println("No response received within the timeout period.");
                }
            } catch (Exception e) {
                System.out.println("Error connecting to or communicating with server " + serverIP + ": " + e.getMessage());
            }

            // Get the next server in the ring
            serverInfo = hashRing.getNextServer(hash);
            attempt++;
        }

        return new Packet(States.LIST_UPDATE_FAILED, "Unable to connect to any DB server");
    }

    private String getListIDFromPacket(Packet requestPacket) {
        if (requestPacket.getState().equals(States.RETRIEVE_LIST_REQUESTED)) {
            System.out.println("[LOG] Requested list ID: " + requestPacket.getMessageBody());
            return requestPacket.getMessageBody();
        } else {
            ShoppingList list = gson.fromJson(requestPacket.getMessageBody(), ShoppingList.class);
            System.out.println("[LOG] Requested list ID: " + list.getListId());
            return list.getListId().toString();
        }
    }

    private void discoverAndConnectToServers() {
        int startPort = 5556;
        int numberOfServers = 10;

        for (int i = 0; i < numberOfServers; i++) {
            int currentPort = startPort + i;
            String address = "tcp://localhost:" + currentPort;

            if (tryConnect(address)) {
                hashRing.addServer(address);
            }
        }
        updateHashRingOnServers();
    }

    private boolean tryConnect(String address) {
        try (ZMQ.Socket testSocket = context.createSocket(SocketType.DEALER)) {
            testSocket.connect(address);
            testSocket.send("ping");

            // Wait for a response with a timeout
            ZMQ.Poller poller = context.createPoller(1);
            poller.register(testSocket, ZMQ.Poller.POLLIN);
            boolean hasReply = poller.poll(200) > 0; // 1 second timeout

            if (hasReply) {
                String reply = testSocket.recvStr();
                return "pong".equals(reply);
            } else {
                return false;
            }
        } catch (Exception e) {
            System.out.println("Unable to connect to server at " + address + ": " + e.getMessage());
            return false;
        }
    }

    private void updateHashRingOnServers() {
        String hashRingData = gson.toJson(hashRing);
        List<String> serverAddresses = hashRing.getAllServerAddresses();

        Set<String> uniqueAddresses = new HashSet<>(serverAddresses);
        System.out.println("[LOG] Sending hash ring update to " + uniqueAddresses.size() + " servers");

        for (String address : uniqueAddresses) {
            try (ZMQ.Socket socket = context.createSocket(SocketType.DEALER)) {
                socket.setSendTimeOut(1000);
                socket.connect(address);

                Packet hashRingUpdatePacket = new Packet(States.HASH_RING_UPDATE, hashRingData);
                String message = gson.toJson(hashRingUpdatePacket);

                boolean sent = socket.send(message.getBytes(ZMQ.CHARSET), 0);
                if (!sent) {
                    throw new IOException("Failed to send message within the timeout period.");
                }

                ZMQ.Poller poller = context.createPoller(1);
                poller.register(socket, ZMQ.Poller.POLLIN);
                int events = poller.poll(2000); // Wait for 2 seconds for an acknowledgment

                if (events > 0) {
                    // We have a reply
                    String reply = socket.recvStr();
                    System.out.println("[LOG] Received hash ring update acknowledgment from server " + address);
                }

                poller.close();
            } catch (Exception e) {
                System.out.println("Error sending hash ring update to server " + address + ": " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        RunRouterServer loadBalancer = new RunRouterServer(5555);
        loadBalancer.run();
    }
}
