package org.example.DBServer;

public class SimpleHashFunction implements HashFunction {
    @Override
    public int hash(String key) {
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }
}
