package land.chipmunk.chayapak.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Main;

import java.io.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PersistentDataUtilities {
    public static File file = new File("persistent.json");

    private static FileWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    private static final ScheduledExecutorService executor = Main.executor;

    private static ScheduledFuture<?> future = null;

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

        future = executor.scheduleAtFixedRate(PersistentDataUtilities::write, 0, 100, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    future.cancel(true);

                    write();
                })
        );
    }

    private static void write () {
        try {
            writer.close();

            writer = new FileWriter(file, false);

            writer.write(jsonObject.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void put (String property, JsonElement value) {
        if (jsonObject.has(property)) jsonObject.remove(property);
        jsonObject.add(property, value);
    }

    public static synchronized void put (String property, String value) {
        if (jsonObject.has(property)) jsonObject.remove(property);
        jsonObject.addProperty(property, value);
    }

    public static synchronized void put (String property, boolean value) {
        if (jsonObject.has(property)) jsonObject.remove(property);
        jsonObject.addProperty(property, value);
    }

    public static synchronized void put (String property, int value) {
        if (jsonObject.has(property)) jsonObject.remove(property);
        jsonObject.addProperty(property, value);
    }

    public static synchronized void put (String property, char value) {
        if (jsonObject.has(property)) jsonObject.remove(property);
        jsonObject.addProperty(property, value);
    }
}
