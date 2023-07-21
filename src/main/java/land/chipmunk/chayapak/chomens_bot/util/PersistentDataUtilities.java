package land.chipmunk.chayapak.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import land.chipmunk.chayapak.chomens_bot.Main;

import java.io.*;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PersistentDataUtilities {
    public static final File file = new File("persistent.json");

    private static FileWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    private static final Map<String, JsonElement> queue = new LinkedHashMap<>();

    private static final ScheduledFuture<?> future;

    static {
        init();

        future = Main.executor.scheduleAtFixedRate(() -> {
            // TODO: thread-safe
            try {
                if (queue.size() == 0) return;

                final Map.Entry<String, JsonElement> entry = queue.entrySet().iterator().next(); // is this the best way to get the first item of the map?

                final String property = entry.getKey();
                final JsonElement value = entry.getValue();

                if (jsonObject.has(property)) jsonObject.remove(property);
                jsonObject.add(property, value);

                write(jsonObject.toString());

                queue.remove(property);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 100, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                future.cancel(false);
            }
        });
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

    private static void write (String string) {
        try {
            writer.close();

            // ? how do i clear the file contents without making a completely new FileWriter
            //   or is this the only way?
            writer = new FileWriter(file, false);

            writer.write(string);
            writer.flush();
        } catch (IOException ignored) {}
    }

    public static synchronized void put (String property, JsonElement value) {
        queue.put(property, value);
    }

    public static synchronized void put (String property, String value) {
        queue.put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, boolean value) {
        queue.put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, int value) {
        queue.put(property, new JsonPrimitive(value));
    }

    public static synchronized void put (String property, char value) {
        queue.put(property, new JsonPrimitive(value));
    }
}
