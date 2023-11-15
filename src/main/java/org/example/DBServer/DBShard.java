package org.example.DBServer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.ShoppingList.ShoppingList;
import org.example.ShoppingList.ShoppingListManager;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DBShard {
    private final String shardFilePath;
    private final int shardNumber;
    private final Gson gson = new Gson();
    private final ZContext context;
    private final ZMQ.Socket socket;

    public DBShard(String shardFilePath, int shardNumber, int port) {
        this.shardFilePath = shardFilePath;
        this.shardNumber = shardNumber;
        this.context = new ZContext();
        this.socket = context.createSocket(ZMQ.REP);
        this.socket.bind("tcp://*:" + port);
    }

    public void addShoppingList(ShoppingList list) throws IOException {
        List<ShoppingList> lists = getShoppingLists();
        lists.add(list);
        saveShoppingLists(lists);
    }

    public List<ShoppingList> getShoppingLists() throws IOException {
        try (Reader reader = new FileReader(shardFilePath)) {
            Type listType = new TypeToken<ArrayList<ShoppingList>>(){}.getType();
            return gson.fromJson(reader, listType);
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        }
    }

    public void updateShoppingList(ShoppingList updatedList) throws IOException {
        List<ShoppingList> lists = getShoppingLists();
        lists.replaceAll(list -> list.getName().equals(updatedList.getName()) ? updatedList : list);
        saveShoppingLists(lists);
    }

    public void deleteShoppingList(String listName) throws IOException {
        List<ShoppingList> lists = getShoppingLists();
        lists.removeIf(list -> list.getName().equals(listName));
        saveShoppingLists(lists);
    }

    private void saveShoppingLists(List<ShoppingList> lists) throws IOException {
        try (Writer writer = new FileWriter(shardFilePath)) {
            gson.toJson(lists, writer);
        }
    }
}