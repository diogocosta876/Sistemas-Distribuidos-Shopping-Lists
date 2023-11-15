package org.example.Messaging;

public class Packet {
    private States state;
    private String messageBody;

    public Packet(States state, String messageBody) {
        this.state = state;
        this.messageBody = messageBody;
    }

    public States getState() {
        return state;
    }

    public void setState(States state) {
        this.state = state;
    }

    public String getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(String messageBody) {
        this.messageBody = messageBody;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "state=" + state +
                ", messageBody='" + messageBody + '\'' +
                '}';
    }
}