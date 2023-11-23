package org.example;

import org.example.DBServer.DBShard;

import java.io.*;

public class RunDBShard {
    private DBShard shard1;
    private DBShard shard2;
    private DBShard shard3;

    public RunDBShard(int shardNumber) throws IOException {
        // Initialize shards with different file paths and unique ports
        shard1 = new DBShard("./src/main/java/org/example/DBServer/Data/shard" + shardNumber + ".json", shardNumber, 5555 + shardNumber);
        shard2 = new DBShard("./src/main/java/org/example/DBServer/Data/shard" + shardNumber +" .json", shardNumber, 5555 + shardNumber);
        shard3 = new DBShard("./src/main/java/org/example/DBServer/Data/shard" + shardNumber +" .json", shardNumber, 5557 + shardNumber);

        shard1.run();
    }

    public static void main(String[] args) throws IOException {
        RunDBShard dbServer = new RunDBShard();
    }
}
