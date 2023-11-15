package org.example;
import com.google.gson.Gson;
import org.example.Messaging.Packet;
import org.example.Messaging.States;
import org.example.ShoppingList.ShoppingList;
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
            byte[] requestBytes = socket.recv(0);
            String requestString = new String(requestBytes, ZMQ.CHARSET);
            Packet requestPacket = gson.fromJson(requestString, Packet.class);

            System.out.println("Received request: " + requestPacket);

            Packet responsePacket = processRequest(requestPacket);

            String serializedResponse = gson.toJson(responsePacket);
            socket.send(serializedResponse.getBytes(ZMQ.CHARSET), 0);
        }
    }

    private Packet processRequest(Packet requestPacket) {
        States state = requestPacket.getState();
        String message = requestPacket.getMessageBody();

        return switch (state) {
            case HANDSHAKE_INITIATED -> {
                System.out.println("Handshake initiated by client.");
                yield new Packet(States.HANDSHAKE_COMPLETED, "Handshake successful");
            }
            case LIST_UPDATE_REQUESTED -> {
                System.out.println("List update requested.");
                ShoppingList list = gson.fromJson(message, ShoppingList.class);
                list.displayList();
                yield new Packet(States.LIST_UPDATE_COMPLETED, "List updated successfully");
            }
            case LIST_DELETE_REQUESTED -> {
                System.out.println("List deletion requested: " + message);
                yield new Packet(States.LIST_DELETE_COMPLETED, "List deleted successfully");
            }
            default -> {
                System.out.println("Invalid request state: " + state);
                yield new Packet(States.LIST_UPDATE_FAILED, "Invalid request state");
            }
        };
    }

    public static void main(String[] args) {
        RunRouterServer loadBalancer = new RunRouterServer(5555);
        loadBalancer.run();
    }
}
