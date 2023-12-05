package org.example.DBServer;

import java.util.*;

public class HashRing {
    private final SortedMap<Integer, String> ring = new TreeMap<>();

    public HashRing() {
    }

    private int hash(String key) {
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    public void addServer(String server) {
        int vnodeCount = 2; // Number of virtual nodes for each server
        for (int i = 0; i < vnodeCount; i++) {
            String vnodeKey = i + server + "#" + i;
            int hash = hash(vnodeKey);
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
        int hash = hash(key.toString());
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

    //get all servers in list
    public List<String> getAllServerAddresses() {
        List<String> servers = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : ring.entrySet()) {
            String server = entry.getValue();
            servers.add(server);
        }
        return servers;
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