package org.example;
import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.*;

public class RunRouterServer {
    private ZMQ.Socket clientSocket; // ROUTER for client
    private ZMQ.Socket dbSocket;     // DEALER for DB
    private final Gson gson;

    public RunRouterServer(int port) {
        ZContext context = new ZContext(1);

        this.clientSocket = context.createSocket(SocketType.ROUTER);
        this.clientSocket.bind("tcp://*:" + port);

        this.dbSocket = context.createSocket(SocketType.DEALER);

        this.dbSocket.connect("tcp://localhost:5556");
        // ... connect to other DB servers as needed ...

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
        dbSocket.send(requestString.getBytes(ZMQ.CHARSET), 0);
        System.out.println("[LOG] Request forwarded to DB server");

        byte[] responseBytes = dbSocket.recv(0);
        String responseString = new String(responseBytes, ZMQ.CHARSET);
        System.out.println("[LOG] Received response from DB server: " + responseString);
        return gson.fromJson(responseString, Packet.class);
    }


    public static void main(String[] args) {
        RunRouterServer loadBalancer = new RunRouterServer(5555);
        loadBalancer.run();
    }
}
