package me.chayapak1.chomens_bot.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PersistentDataUtilities {
    private static final Path path = Path.of("persistent.json");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static ObjectNode jsonObject = JsonNodeFactory.instance.objectNode();

    private static final ReentrantLock lock = new ReentrantLock();

    private static ScheduledFuture<?> writeFuture;

    private static volatile boolean stopping = false;

    public static void init () {
        lock.lock();

        try {
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    jsonObject = (ObjectNode) objectMapper.readTree(reader);
                }
            } else {
                Files.createFile(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        writeFuture = Main.executor.scheduleAtFixedRate(PersistentDataUtilities::writeToFile, 10, 30, TimeUnit.SECONDS);
    }

    private static synchronized void writeToFile () { writeToFile(false); }
    private static synchronized void writeToFile (boolean force) {
        if (stopping && !force) return;

        ObjectNode safeCopy;

        lock.lock();

        try {
            safeCopy = jsonObject.deepCopy();
        } finally {
            lock.unlock();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(objectMapper.writeValueAsString(safeCopy));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void stop () {
        stopping = true;

        writeFuture.cancel(false);

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

    public static synchronized JsonNode get (String property) {
        return jsonObject.get(property);
    }

    public static synchronized JsonNode getOrDefault (String property, JsonNode defaultElement) {
        return has(property) ? get(property) : defaultElement;
    }

    public static synchronized void put (String property, JsonNode value) {
        lock.lock();

        try {
            jsonObject.set(property, value);
        } finally {
            lock.unlock();
        }
    }

    public static synchronized void put (String property, String value) {
        put(property, JsonNodeFactory.instance.textNode(value));
    }

    public static synchronized void put (String property, boolean value) {
        put(property, JsonNodeFactory.instance.booleanNode(value));
    }

    public static synchronized void put (String property, int value) {
        put(property, JsonNodeFactory.instance.numberNode(value));
    }

    public static synchronized void put (String property, char value) {
        put(property, JsonNodeFactory.instance.textNode(String.valueOf(value)));
    }
}
