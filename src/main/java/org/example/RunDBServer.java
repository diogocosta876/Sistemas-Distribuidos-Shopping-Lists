package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.DBServer.DBShard;
import org.example.ShoppingList.ShoppingList;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RunDBServer {
    private DBShard shard1;
    private DBShard shard2;
    private DBShard shard3;

    public RunDBServer() {
        // Initialize shards with different file paths and unique ports
        shard1 = new DBShard("shard1.json", 1, 5551);
        shard2 = new DBShard("shard2.json", 2, 5552);
        shard3 = new DBShard("shard3.json", 3, 5553);
    }

}
