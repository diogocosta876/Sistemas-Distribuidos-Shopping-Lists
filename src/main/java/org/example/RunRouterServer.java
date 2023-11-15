package org.example;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class RunRouterServer {
    private final ZMQ.Socket socket;

    public RunRouterServer(int port) {
        ZContext context = new ZContext(1);
        this.socket = context.createSocket(SocketType.REP);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() {

        while (!Thread.currentThread().isInterrupted()) {


            System.out.println("Server Running");
            // Receive client request
            //byte[] clientId = socket.recv(0);
            byte[] request = socket.recv(0);

            String Nrequest = new String(request, ZMQ.CHARSET);
            System.out.println("Request:  "+ Nrequest);

            // Simulate processing
            String response = "Response from Load Balancer";

            // Reply to the client
            //socket.sendMore(clientId);
            socket.send(response.getBytes(ZMQ.CHARSET), 0);
        }
    }


    //For independant execution
    public static void main(String[] args) {
        // Example usage:
        RunRouterServer loadBalancer = new RunRouterServer(5555);
        loadBalancer.run();
    }
}
