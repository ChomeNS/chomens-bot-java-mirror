package me.chayapak1.chomensbot_mabe;

import me.chayapak1.chomensbot_mabe.plugins.ConsolePlugin;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        final File file = new File("config.yml");
        final Constructor constructor = new Constructor(Configuration.class);
        final Yaml yaml = new Yaml(constructor);
        Configuration _config;

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

            _config = yaml.load(is);
        }

        InputStream opt = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        _config = yaml.load(reader);

        final Configuration config = _config;

        Configuration.Bots[] botsOptions = config.bots();

        final List<Bot> allBots = new ArrayList<>();

        final CountDownLatch latch = new CountDownLatch(botsOptions.length);

        for (Configuration.Bots botOption : botsOptions) {
            final String host = botOption.host();
            final int port = botOption.port();
            final String username = botOption.username();

            new Thread(() -> {
                final Bot bot = new Bot(host, port, username, allBots, config);
                allBots.add(bot);

                latch.countDown();
            }).start();
        }

        latch.await();
        new ConsolePlugin(allBots);
    }
}