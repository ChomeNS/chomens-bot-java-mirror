package me.chayapak1.chomens_bot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.plugins.ConsolePlugin;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final List<Bot> bots = new ArrayList<>();

    public static final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("ExecutorService #%d").build()
    );
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("ScheduledExecutorService #%d").build()
    );

    private static Configuration config;

    private static boolean alreadyStarted = false;

    private static boolean stopping = false;

    private static final List<Thread> alreadyAddedThreads = new ArrayList<>();

    private static JDA jda = null;

    public static void main(String[] args) throws IOException {
        final Path configPath = Path.of("config.yml");

        final Constructor constructor = new Constructor(Configuration.class, new LoaderOptions());
        final Yaml yaml = new Yaml(constructor);

        if (!Files.exists(configPath)) {
            // creates config file from default-config.yml
            InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.yml");

            if (is == null) System.exit(1);

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();
            while (reader.ready()) {
                char character = (char) reader.read();
                stringBuilder.append(character);
            }
            String defaultConfig = stringBuilder.toString();

            // writes it
            BufferedWriter configWriter = Files.newBufferedWriter(configPath);
            configWriter.write(defaultConfig);
            configWriter.close();

            LoggerUtilities.info("config.yml file not found, so the default one was created");
        }

        InputStream opt = Files.newInputStream(configPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);

        PersistentDataUtilities.init();
        ComponentUtilities.stringify(Component.empty()); // best way to initialize the class 2024

        executor.scheduleAtFixedRate(() -> {
            try {
                checkInternet();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, 0, 1, TimeUnit.MINUTES);

        executor.scheduleAtFixedRate(() -> {
            final Set<Thread> threads = Thread.getAllStackTraces().keySet();

            for (Thread thread : threads) {
                final Thread.UncaughtExceptionHandler oldHandler = thread.getUncaughtExceptionHandler();

                thread.setUncaughtExceptionHandler((_thread, throwable) -> {
                    if (!alreadyAddedThreads.contains(thread) && throwable instanceof OutOfMemoryError) System.exit(1);

                    alreadyAddedThreads.add(thread);

                    oldHandler.uncaughtException(_thread, throwable);
                });
            }
        }, 0, 30, TimeUnit.SECONDS);

        if (!config.backup.enabled) {
            initializeBots();
            return;
        }

        executor.scheduleAtFixedRate(() -> {
            boolean reachable;

            try {
                HttpUtilities.getRequest(new URL(config.backup.address));

                reachable = true;
            } catch (Exception e) {
                reachable = false;
            }

            if (!reachable && !alreadyStarted) {
                LoggerUtilities.info("Main instance is down! Starting backup instance");

                initializeBots();
            } else if (reachable && alreadyStarted) {
                System.exit(1);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public static void initializeBots() {
        alreadyStarted = true;

        try {
            Configuration.BotOption[] botsOptions = config.bots;

            // idk if these should be here lol, but it is just the discord stuff
            if (config.discord.enabled) {
                JDABuilder builder = JDABuilder.createDefault(config.discord.token);
                builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
                try {
                    jda = builder.build();
                    jda.awaitReady();
                } catch (InterruptedException ignored) {
                    System.exit(1);
                }
                jda.getPresence().setPresence(Activity.playing(config.discord.statusMessage), false);
            }

            for (Configuration.BotOption botOption : botsOptions) {
                final Bot bot = new Bot(botOption, bots, config);
                bots.add(bot);
            }

            // fard
            new ConsolePlugin(bots, config, jda);
        } catch (Exception e) {
            e.printStackTrace();

            System.exit(1);
        }
    }

    private static void checkInternet () throws IOException {
        if (!config.internetCheck.enabled) return;

        boolean reachable = false;

        try {
            final URL url = new URL(config.internetCheck.address);

            final URLConnection connection = url.openConnection();

            connection.connect();

            reachable = true;
        } catch (UnknownHostException ignored) {}

        if (!reachable) {
            LoggerUtilities.error("No internet, exiting");

            System.exit(1);
        }
    }

    // most of these are stolen from HBot
    public static void stop () {
        if (stopping) return;

        stopping = true;

        executor.shutdown();

        PersistentDataUtilities.stop();

        executorService.shutdown();

        try {
            final boolean executorDone = executor.awaitTermination(5, TimeUnit.SECONDS);
            final boolean executorServiceDone = executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ArrayList<Bot> copiedList;
        synchronized (bots) {
            copiedList = new ArrayList<>(bots);
        }

        final boolean ircEnabled = config.irc.enabled;
        final boolean discordEnabled = config.discord.enabled;

        for (Bot bot : copiedList) {
            try {
                if (discordEnabled) {
                    final String channelId = bot.discord.servers.get(bot.host + ":" + bot.port);

                    bot.discord.sendMessageInstantly("Stopping..", channelId);
                }

                if (ircEnabled) bot.irc.quit("Stopping..");

                bot.stop();
            } catch (Exception ignored) {}
        }

        if (jda != null) jda.shutdown();

        if (discordEnabled) {
            for (int i = 0; i < 150; i++) {
                boolean stoppedDiscord = true;

                for (Bot bot : bots) {
                    if (!bot.discord.shuttedDown) {
                        stoppedDiscord = false;
                        break;
                    }
                }

                if (!stoppedDiscord) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        System.exit(69); // nice
    }
}