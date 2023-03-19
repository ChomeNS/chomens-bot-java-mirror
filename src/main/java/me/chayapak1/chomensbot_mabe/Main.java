package me.chayapak1.chomensbot_mabe;

import me.chayapak1.chomensbot_mabe.plugins.ConsolePlugin;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        final File file = new File("config.yml");
        final Yaml yaml = new Yaml();
        Map<String, List<Map<String, Object>>> config;

        if (!file.exists()) {
            // creates config file from default-config.yml
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.yml");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            while (reader.ready()) {
                char character = (char) reader.read();
                stringBuilder.append(character);
            }
            String defaultConfig = stringBuilder.toString();

            // writes it
            BufferedWriter configWriter = new BufferedWriter(new FileWriter(file));
            configWriter.write(defaultConfig);
            configWriter.close();

            System.out.println("config.yml file not found, so the default one was created");

            config = yaml.load(is);
        }

        InputStream opt = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);

        final Object reconnectDelayObject = config.get("reconnectDelay");
        final int reconnectDelay = (int) reconnectDelayObject;

        List<Map<String, Object>> botsOptions = config.get("bots");

        final List<Bot> allBots = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(botsOptions.size());

        for (Map<String, Object> botOption : botsOptions) {
            final String host = (String) botOption.get("host");
            final int port = (int) botOption.get("port");
            final String username = (String) botOption.get("username");

            new Thread(() -> {
                final Bot bot = new Bot(host, port, reconnectDelay, username, allBots);
                allBots.add(bot);

                latch.countDown();
            }).start();
        }

        latch.await();
        new ConsolePlugin(allBots);
    }
}