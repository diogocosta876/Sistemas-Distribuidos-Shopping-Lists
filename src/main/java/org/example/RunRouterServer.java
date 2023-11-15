package org.example;
import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class RunRouterServer {
    private ZMQ.Socket clientSocket;
    private ZMQ.Socket dbSocket;
    private final Gson gson;

    public RunRouterServer(int port) {
        ZContext context = new ZContext(1);

        this.clientSocket = context.createSocket(SocketType.REP);
        this.clientSocket.bind("tcp://*:" + port);

        this.dbSocket = context.createSocket(SocketType.REQ);
        String dbServerAddress = "tcp://localhost:5556";
        this.dbSocket.connect(dbServerAddress);
        gson = new Gson();
    }

    public void run() {
        System.out.println("Server Running");
        while (!Thread.currentThread().isInterrupted()) {
            byte[] requestBytes = clientSocket.recv(0);
            String requestString = new String(requestBytes, ZMQ.CHARSET);
            Packet requestPacket = gson.fromJson(requestString, Packet.class);

            System.out.println("[LOG] Received request: " + requestPacket);

            Packet responsePacket = processRequest(requestPacket);

            String serializedResponse = gson.toJson(responsePacket);
            clientSocket.send(serializedResponse.getBytes(ZMQ.CHARSET), 0);
            System.out.println("[LOG] Sent Client response: " + responsePacket);
        }
    }

    private Packet processRequest(Packet requestPacket) {
        switch (requestPacket.getState()) {
            case HANDSHAKE_INITIATED:
                System.out.println("[LOG] Handshake initiated by client.");
                return new Packet(States.HANDSHAKE_COMPLETED, "[LOG] Handshake successful");

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
