package org.example.DBServer;

public class SimpleHashFunction implements HashFunction {
    @Override
    public int hash(String key) {
        return key.hashCode();
    }
}
