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

public class PersistentDataUtilities {
    public static final Path path = Path.of("persistent.json");

    private static BufferedWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    private static boolean stopping = false;

    static {
        init();
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

            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void write (String string) {
        if (stopping) return; // is this necessary?

        try {
            writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            writer.write(string);
            writer.flush();
        } catch (IOException ignored) {}
    }

    public static void stop () {
        stopping = true;
        write(jsonObject.toString());
    }

    public static void put (String property, JsonElement value) {
        jsonObject.add(property, value);
        write(jsonObject.toString());
    }

    public static void put (String property, String value) {
        jsonObject.add(property, new JsonPrimitive(value));
        write(jsonObject.toString());
    }

    public static void put (String property, boolean value) {
        jsonObject.add(property, new JsonPrimitive(value));
        write(jsonObject.toString());
    }

    public static void put (String property, int value) {
        jsonObject.add(property, new JsonPrimitive(value));
        write(jsonObject.toString());
    }

    public static void put (String property, char value) {
        jsonObject.add(property, new JsonPrimitive(value));
        write(jsonObject.toString());
    }
}
