package me.chayapak1.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import me.chayapak1.chomens_bot.Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PersistentDataUtilities {
    public static final Path path = Path.of("persistent.json");

    private static BufferedWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    private static final Map<String, JsonElement> queue = new ConcurrentHashMap<>();

    private static final ScheduledFuture<?> future;

    static {
        init();

        future = Main.executor.scheduleAtFixedRate(() -> {
            try {
                if (queue.isEmpty()) return;

                final Map.Entry<String, JsonElement> entry = queue.entrySet().iterator().next(); // is this the best way to get the first item of the map?

                final String property = entry.getKey();
                final JsonElement value = entry.getValue();

                Main.executorService.submit(() -> {
                    jsonObject.add(property, value);

                    write(jsonObject.toString());

                    queue.remove(property);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private static void init () {
        try {
            if (!Files.exists(path)) Files.createFile(path);

            // loads the persistent data from the last session
            else {
                final BufferedReader reader = Files.newBufferedReader(path);

                final Gson gson = new Gson();

                jsonObject = gson.fromJson(reader, JsonObject.class);
            }

            writer = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write (String string) {
        try {
            writer.close();

            // ? how do i clear the file contents without making a completely new writer?
            //   or is this the only way?
            writer = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING);

            writer.write(string);
            writer.flush();
        } catch (IOException ignored) {}
    }

    public static void stop () {
        future.cancel(false);
        write(jsonObject.toString());
    }

    public static void put (String property, JsonElement value) {
        Main.executorService.submit(() -> queue.put(property, value));
    }

    public static void put (String property, String value) {
        Main.executorService.submit(() -> queue.put(property, new JsonPrimitive(value)));
    }

    public static void put (String property, boolean value) {
        Main.executorService.submit(() -> queue.put(property, new JsonPrimitive(value)));
    }

    public static void put (String property, int value) {
        Main.executorService.submit(() -> queue.put(property, new JsonPrimitive(value)));
    }

    public static void put (String property, char value) {
        Main.executorService.submit(() -> queue.put(property, new JsonPrimitive(value)));
    }
}
