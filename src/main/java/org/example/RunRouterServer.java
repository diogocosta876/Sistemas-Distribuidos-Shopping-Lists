package org.example;
import com.google.gson.Gson;
import org.example.DBServer.HashFunction;
import org.example.DBServer.HashRing;
import org.example.DBServer.SimpleHashFunction;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.*;

import java.util.List;

public class RunRouterServer {
    private final ZMQ.Socket clientSocket;
    private final ZContext context = new ZContext(1);
    private final Gson gson;
    private HashRing hashRing;

    public RunRouterServer(int port, HashFunction hashFunction, int numberOfShardReplicas) {

        this.clientSocket = context.createSocket(SocketType.ROUTER);
        this.clientSocket.bind("tcp://*:" + port);

        this.hashRing = new HashRing(hashFunction, numberOfShardReplicas);
        hashRing.addServer("tcp://localhost:5556");
        hashRing.addServer("tcp://localhost:5557");

        gson = new Gson();
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

            case RETRIEVE_LISTS_REQUESTED:
            case LIST_UPDATE_REQUESTED:
            case LIST_DELETE_REQUESTED:
                return forwardRequestToDBServer(requestPacket);

            default:
                System.out.println("Invalid request state: " + requestPacket.getState());
                return new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
        }
    }

    private Packet forwardRequestToDBServer(Packet requestPacket) {
        String requestString = gson.toJson(requestPacket);

        //TODO fetch list id from request packet if request involves list
        String shardKey = "31142cc1-ca15-4ce3-8f46-87f0da0972a6";

        List<String> servers = hashRing.getServers(shardKey);
        String primaryServer = servers.get(0); //TODO implement hashing algorithm to select primary server

        ZMQ.Socket dbSocket = context.createSocket(SocketType.DEALER);
        dbSocket.connect(primaryServer);

        dbSocket.send(requestString.getBytes(ZMQ.CHARSET), 0);
        System.out.println("[LOG] Request forwarded to DB server");

        byte[] responseBytes = dbSocket.recv(0);
        String responseString = new String(responseBytes, ZMQ.CHARSET);
        System.out.println("[LOG] Received response from DB server: " + responseString);
        return gson.fromJson(responseString, Packet.class);
    }



    public static void main(String[] args) {
        RunRouterServer loadBalancer = new RunRouterServer(5555, new SimpleHashFunction(), 1 );
        loadBalancer.run();
    }
}
