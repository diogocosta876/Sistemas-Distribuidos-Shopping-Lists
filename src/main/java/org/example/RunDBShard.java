package org.example;

import org.example.DBServer.DBShard;

import java.io.*;
import java.util.Scanner;

public class RunDBShard {
    private DBShard shard1;

    public RunDBShard(int shardNumber) throws IOException {
        // Initialize shards with different file paths and unique ports
        shard1 = new DBShard("./src/main/java/org/example/DBServer/Data/shard" + shardNumber + ".json", shardNumber, 5555 + shardNumber);

        shard1.run();
    }

    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter shard number to run: ");
        int shardNumber = scanner.nextInt();

        RunDBShard dbServer = new RunDBShard(shardNumber);
        scanner.close();
    }
}