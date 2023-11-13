package org.example;

import org.zeromq.ZMQ;

public class MiddleManServer {
    private final ZMQ.Socket socket;

    public MiddleManServer(int port) {
        ZMQ.Context context = ZMQ.context(1);
        this.socket = context.socket(ZMQ.ROUTER);
        this.socket.bind("tcp://*:" + port);
    }

    public void run() {

        while (!Thread.currentThread().isInterrupted()) {


            System.out.println("Server Running");
            // Receive client request
            byte[] clientId = socket.recv(0);
            byte[] request = socket.recv(0);

            String Nrequest = new String(request, ZMQ.CHARSET);
            System.out.println("Request: "+Nrequest);

            // Simulate processing
            String response = "Response from Load Balancer";

            // Reply to the client
            socket.sendMore(clientId);
            socket.send(response.getBytes(ZMQ.CHARSET), 0);
        }
    }


    //For independant execution
    public static void main(String[] args) {
        // Example usage:
        MiddleManServer loadBalancer = new MiddleManServer(5555);
        loadBalancer.run();
    }
}
