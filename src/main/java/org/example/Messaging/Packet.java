package org.example.Messaging;

import java.util.Map;

public class Packet {
    private States state;
    private String messageBody;

    private Map<String,Integer> extraInfo;

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


    public Map<String,Integer> getExtraInfo(){
        return extraInfo;
    }
    public void setExtraInfo(Map<String,Integer> info){
        this.extraInfo = info;
    }


    @Override
    public String toString() {
        return "Packet{" +
                "state=" + state +
                ", messageBody='" + messageBody + '\'' +
                '}';
    }
}