package org.example;

import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class RunRouterServer {
    private ZMQ.Socket socket;
    private final Gson gson;

    public RunRouterServer(int port) {
        ZContext context = new ZContext(1);
        this.socket = context.createSocket(SocketType.REP);
        this.socket.bind("tcp://*:" + port);
        gson = new Gson();
    }

    public void run() {
        System.out.println("Server Running");
        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("Awaiting request...");

            byte[] requestBytes = socket.recv(0);
            String requestString = new String(requestBytes, ZMQ.CHARSET);
            Packet requestPacket = gson.fromJson(requestString, Packet.class);

            Packet responsePacket = processRequest(requestPacket);

            String serializedResponse = gson.toJson(responsePacket);
            socket.send(serializedResponse.getBytes(ZMQ.CHARSET), 0);
        }
    }

    private Packet processRequest(Packet requestPacket) {
        if (requestPacket.getState() == States.HANDSHAKE_INITIATED) {
            System.out.println("Handshake initiated by client.");

            return new Packet(States.HANDSHAKE_COMPLETED, "Handshake successful");
        }


        return new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
    }

    public static void main(String[] args) {
        // Example usage:
        RunRouterServer loadBalancer = new RunRouterServer(5555);
        loadBalancer.run();
    }
}
