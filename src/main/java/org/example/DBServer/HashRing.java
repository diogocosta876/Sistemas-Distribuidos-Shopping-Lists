package org.example.DBServer;

import java.util.*;

public class HashRing {
    private final SortedMap<Integer, List<String>> ring = new TreeMap<>();
    private final HashFunction hashFunction;
    private final int numberOfReplicas; // Number of replicas for each shard

    public HashRing(HashFunction hashFunction, int numberOfReplicas) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;
    }

    public void addServer(String server) {
        int vnodeCount = 100; // Number of virtual nodes for each server
        for (int i = 0; i < vnodeCount; i++) {
            String vnodeKey = i + server + "#" + i;
            int hash = hashFunction.hash(vnodeKey);
            List<String> servers = ring.getOrDefault(hash, new ArrayList<>());
            servers.add(server);
            ring.put(hash, servers);
        }
    }

    public void removeServer(String server) {
        for (int i = 0; i < numberOfReplicas; i++) {
            int hash = hashFunction.hash(server + i);
            List<String> servers = ring.get(hash);
            if (servers != null) {
                servers.remove(server);
                if (servers.isEmpty()) {
                    ring.remove(hash);
                }
            }
        }
    }

    public List<String> getServers(Object key) {
        if (ring.isEmpty()) {
            return Collections.emptyList();
        }
        int hash = hashFunction.hash(key.toString());
        System.out.println("request Hash: " + hash);
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, List<String>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        System.out.println("chosen shard Hash: " + hash);
        return new ArrayList<>(ring.get(hash));
    }

    public String displayAllServers() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, List<String>> entry : ring.entrySet()) {
            int hash = entry.getKey();
            List<String> servers = entry.getValue();
            sb.append("Hash: ").append(hash).append(" - Servers: ").append(servers.toString()).append("\n");
        }
        return sb.toString();
    }
}