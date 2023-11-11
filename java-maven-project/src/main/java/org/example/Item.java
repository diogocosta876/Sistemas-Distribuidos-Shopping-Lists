package org.example;

import java.io.Serializable;

public class Item implements Serializable {
    private String name;
    private int quantity;

    public Item(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    // Getters and setters

    public String getName(){
        return this.name;
    }

    public int getQuantity(){
        return this.quantity;
    }
}
