package me.chayapak1.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReentrantLock;

public class PersistentDataUtilities {
    private static final Path path = Path.of("persistent.json");

    private static final Gson gson = new Gson();

    private static JsonObject jsonObject = new JsonObject();

    private static final ReentrantLock lock = new ReentrantLock();

    private static volatile boolean stopping = false;

    public static void init () {
        lock.lock();

        try {
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    jsonObject = gson.fromJson(reader, JsonObject.class);
                }
            } else {
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    private static synchronized void writeToFile () { writeToFile(false); }
    private static synchronized void writeToFile (boolean force) {
        if (stopping && !force) return;

        lock.lock();

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(gson.toJson(jsonObject));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public static synchronized void stop () {
        stopping = true;

        lock.lock();

        try {
            writeToFile(true);
        } finally {
            lock.unlock();
        }
    }

    public static synchronized boolean has (String property) {
        return jsonObject.has(property);
    }

    public static synchronized JsonElement get (String property) {
        return jsonObject.get(property);
    }

    public static synchronized JsonElement getOrDefault (String property, JsonElement defaultElement) {
        return has(property) ? get(property) : defaultElement;
    }

    public static synchronized void put (String property, JsonElement value) {
        lock.lock();

        try {
            jsonObject.add(property, value);
            writeToFile();
        } finally {
            lock.unlock();
        }
    }

    public static synchronized void put (String property, String value) {
        put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, boolean value) {
        put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, int value) {
        put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, char value) {
        put(property, new JsonPrimitive(value));
    }
}
