package org.example.ShoppingList;

import java.io.Serializable;

public class CRDTItem implements Serializable {



    private int quantity;
    private String userId;
    private long timestamp;

    public CRDTItem(int quantity, String userId) {

        this.quantity = 0;
        this.userId = "";
        this.timestamp = 0;
    }

    // Update the item with new quantity, user ID, and timestamp
    public void updateItem(int quantity, String userId, long timestamp) {
        if (timestamp > this.timestamp || (timestamp == this.timestamp && userId.compareTo(this.userId) > 0)) {
            this.quantity = quantity;
            this.userId = userId;
            this.timestamp = timestamp;
        }
    }

    public void merge(CRDTItem otherItem) {
        if (otherItem.timestamp > this.timestamp || (otherItem.timestamp == this.timestamp && otherItem.userId.compareTo(this.userId) > 0)) {
            this.quantity = otherItem.quantity;
            this.userId = otherItem.userId;
            this.timestamp = otherItem.timestamp;
        }
    }

    public int getQuantity() {
        return quantity;
    }

    // Get the user ID of the last updater
    public String getUserId() {
        return userId;
    }

    public long getTimeStamp(){return timestamp;}
}
