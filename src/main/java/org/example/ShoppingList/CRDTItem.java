package org.example.ShoppingList;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CRDTItem implements Serializable {
    private String itemName;
    private int quantity;
    private long timestamp;
    private UUID userId; //the user id of the last user to change this item

    public CRDTItem(String itemName, int quantity, long timestamp, UUID userId) {
        this.itemName = itemName;
        this.quantity = quantity;
        this.timestamp = timestamp;
        this.userId = userId;
    }



    public String getItemName(){return itemName;}

    public UUID getUserId(){return userId;}

    public long getTimestamp(){return timestamp;}

    public int getQuantity() {return quantity;}

    public void setQuantity(int newQuantity){ quantity = newQuantity;}

    public void setTimestamp(long timestamp) {this.timestamp = timestamp;}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CRDTItem item = (CRDTItem) o;
        return itemName.equals(item.itemName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName);
    }
}
