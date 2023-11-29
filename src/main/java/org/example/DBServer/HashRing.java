package org.example.DBServer;

import java.util.*;

public class HashRing {
    private final SortedMap<Integer, String> ring = new TreeMap<>();
    private final HashFunction hashFunction;

    public HashRing(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    public void addServer(String server) {
        int vnodeCount = 2; // Number of virtual nodes for each server
        for (int i = 0; i < vnodeCount; i++) {
            String vnodeKey = i + server + "#" + i;
            int hash = hashFunction.hash(vnodeKey);
            ring.put(hash, server);
        }
    }

    public void removeServer(String server) {
        Iterator<Map.Entry<Integer, String>> iterator = ring.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            if (entry.getValue().equals(server)) {
                iterator.remove();
            }
        }
    }

    public Map<Integer, String> getServer(Object key) {
        if (ring.isEmpty()) {
            return Collections.emptyMap();
        }
        int hash = hashFunction.hash(key.toString());
        SortedMap<Integer, String> tailMap = ring.tailMap(hash);
        hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        String server = ring.get(hash);

        Map<Integer, String> result = new HashMap<>();
        result.put(hash, server);
        return result;
    }

    public Map<Integer, String> getNextServer(int hash) {
        // Find the next key after the given hash
        SortedMap<Integer, String> tailMap = ring.tailMap(hash + 1);
        Integer nextHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();

        String serverIP = ring.get(nextHash);
        Map<Integer, String> result = new HashMap<>();
        result.put(nextHash, serverIP);
        return result;
    }

    public Map<Integer, String> getPreviousServer(int hash) {
        // Find the previous key before the given hash
        SortedMap<Integer, String> headMap = ring.headMap(hash);
        Integer previousHash = headMap.isEmpty() ? ring.lastKey() : headMap.lastKey();

        String serverIP = ring.get(previousHash);
        Map<Integer, String> result = new HashMap<>();
        result.put(previousHash, serverIP);
        return result;
    }

    public String displayAllServers() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> entry : ring.entrySet()) {
            int hash = entry.getKey();
            String server = entry.getValue();
            sb.append("Hash: ").append(hash).append(" - Server: ").append(server).append("\n");
        }
        return sb.toString();
    }
}