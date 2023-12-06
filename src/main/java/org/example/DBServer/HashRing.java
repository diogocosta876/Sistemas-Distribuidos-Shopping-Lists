package org.example.DBServer;

import java.util.*;

public class HashRing {
    private final SortedMap<Integer, String> ring = new TreeMap<>();

    public HashRing() {
    }

    public SortedMap<Integer, String> getRing() {
        return ring;
    }

    public int hash(String key) {//TODO CHANGE HASH TO PRIVATE
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

    public Map<Integer, String> getNextNthServer(int hash, int n) {
        Integer currentHash = hash;
        String serverIP = null;

        for (int i = 0; i < n; i++) {
            SortedMap<Integer, String> tailMap = ring.tailMap(currentHash + 1);
            currentHash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            serverIP = ring.get(currentHash);
        }

        Map<Integer, String> result = new HashMap<>();
        result.put(currentHash, serverIP);
        return result;
    }

    public Map<Integer, String> getPreviousNthServer(int hash, int n) {
        Integer currentHash = hash;
        String serverIP = null;

        for (int i = 0; i < n; i++) {
            SortedMap<Integer, String> headMap = ring.headMap(currentHash);
            currentHash = headMap.isEmpty() ? ring.lastKey() : headMap.lastKey();
            serverIP = ring.get(currentHash);
        }

        Map<Integer, String> result = new HashMap<>();
        result.put(currentHash, serverIP);
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