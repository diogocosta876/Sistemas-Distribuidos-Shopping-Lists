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
        for (int i = 0; i < numberOfReplicas; i++) {
            int hash = hashFunction.hash(server + i);
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
        if (!ring.containsKey(hash)) {
            SortedMap<Integer, List<String>> tailMap = ring.tailMap(hash);
            hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
        }
        return new ArrayList<>(ring.get(hash));
    }
}