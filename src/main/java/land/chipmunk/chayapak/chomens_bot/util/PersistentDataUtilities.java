package land.chipmunk.chayapak.chomens_bot.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PersistentDataUtilities {
    public static final File file = new File("persistent.json");

    private static FileWriter writer;

    public static JsonObject jsonObject = new JsonObject();

    private static final List<String> queue = new ArrayList<>();

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

            // i already use ExecutorService so is a queue optional?
            Main.executor.scheduleAtFixedRate(() -> {
                if (queue.isEmpty()) return;

                if (queue.size() > 50) queue.clear();

                write(queue.get(0));

                queue.remove(0);
            }, 0, 1, TimeUnit.SECONDS);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                while (true) {
                    if (queue.isEmpty()) break;
                }
            }));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void write (String object) {
        try {
            writer.close();

            // ? how do i clear the file contents without making a completely new FileWriter
            //   or is this the only way?
            writer = new FileWriter(file, false);

            writer.write(object);
            writer.flush();
        } catch (IOException | ConcurrentModificationException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void put (String property, JsonElement value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.add(property, value);

            queue.add(jsonObject.toString());
        });
    }

    public static synchronized void put (String property, String value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            queue.add(jsonObject.toString());
        });
    }

    public static synchronized void put (String property, boolean value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            queue.add(jsonObject.toString());
        });
    }

    public static synchronized void put (String property, int value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            queue.add(jsonObject.toString());
        });
    }

    public static synchronized void put (String property, char value) {
        Main.executorService.submit(() -> {
            if (jsonObject.has(property)) jsonObject.remove(property);
            jsonObject.addProperty(property, value);

            queue.add(jsonObject.toString());
        });
    }
}
