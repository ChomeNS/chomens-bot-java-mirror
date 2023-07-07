package land.chipmunk.chayapak.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Main;

import java.io.*;
import java.util.ConcurrentModificationException;

public class PersistentDataUtilities {
    public static final File file = new File("persistent.json");

    private static FileWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    static {
        init();
    }

    private static void init () {
        try {
            if (!file.exists()) file.createNewFile();

            // loads the persistent data from the last session
            else {
                final InputStream opt = new FileInputStream(file);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

                final Gson gson = new Gson();

                jsonObject = gson.fromJson(reader, JsonObject.class);
            }

            writer = new FileWriter(file, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void write () {
        try {
            final String stringifiedJsonObject = jsonObject.toString();

            writer.close();

            // ? how do i clear the file contents without making a completely new FileWriter
            //   or is this the only way?
            writer = new FileWriter(file, false);

            writer.write(stringifiedJsonObject);
            writer.flush();
        } catch (IOException | ConcurrentModificationException ignored) {}
    }

    public static synchronized void put (String property, JsonElement value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.add(property, value);

            write();
        });
    }

    public static synchronized void put (String property, String value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            write();
        });
    }

    public static synchronized void put (String property, boolean value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            write();
        });
    }

    public static synchronized void put (String property, int value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            write();
        });
    }

    public static synchronized void put (String property, char value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            write();
        });
    }
}
