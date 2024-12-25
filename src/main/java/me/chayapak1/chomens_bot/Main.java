package me.chayapak1.chomens_bot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.plugins.*;
import me.chayapak1.chomens_bot.util.*;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

    private static int backupFailTimes = 0;

    public static DatabasePlugin database;
    private static DiscordPlugin discord;

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

            LoggerUtilities.info("config.yml file was not found, so the default one was created. Please modify it to your needs.");

            System.exit(1);
        }

        InputStream opt = Files.newInputStream(configPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);

        if (!config.backup.enabled) {
            initializeBots();
        } else {
            executor.scheduleAtFixedRate(() -> {
                boolean reachable;

                try {
                    HttpUtilities.getRequest(new URL(config.backup.address));

                    reachable = true;
                } catch (Exception e) {
                    reachable = false;
                }

                if (!reachable && !alreadyStarted) {
                    backupFailTimes++;

                    if (backupFailTimes > config.backup.failTimes) {
                        LoggerUtilities.info("Main instance is down! Starting backup instance");

                        initializeBots();
                    }
                } else if (reachable && alreadyStarted) {
                    LoggerUtilities.info("Main instance is back up! Now stopping");

                    // no need to reset backupFailTimes because we are stopping anyway

                    stop(0);
                }
            }, 0, config.backup.interval, TimeUnit.MILLISECONDS);
        }
    }

    public static void initializeBots() {
        alreadyStarted = true;

        try {
            final Configuration.BotOption[] botsOptions = config.bots;

            for (Configuration.BotOption botOption : botsOptions) {
                final Bot bot = new Bot(botOption, bots, config);
                bots.add(bot);
            }

            // initialize plugins
            new ConsolePlugin();
            LoggerPlugin.init();
            if (config.database.enabled) database = new DatabasePlugin(config);
            if (config.discord.enabled) discord = new DiscordPlugin(config);
            if (config.irc.enabled) new IRCPlugin(config);

            LoggerUtilities.info("Initialized all bots. Now connecting");

            for (Bot bot : bots) bot.connect();
        } catch (Exception e) {
            e.printStackTrace();

            System.exit(1);
        }
    }

    // most of these are stolen from HBot
    public static void stop (int exitCode) { stop(exitCode, null, null); }
    public static void stop (int exitCode, String reason) { stop(exitCode, reason, null); }
    public static void stop (int exitCode, String reason, String type) {
        if (stopping) return;

        stopping = true;

        final String stoppingMessage = String.format(
                "%s (%s)",
                type != null ? type : "Stopping..",
                reason != null ? reason : "No reason given"
        );

        executor.shutdown();

        executorService.shutdown();

        database.stop();

        try {
            final boolean ignoredExecutorDone = executor.awaitTermination(5, TimeUnit.SECONDS);
            final boolean ignoredExecutorServiceDone = executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        ArrayList<Bot> copiedList;
        synchronized (bots) {
            copiedList = new ArrayList<>(bots);
        }

        final boolean ircEnabled = config.irc.enabled;
        final boolean discordEnabled = config.discord.enabled;

        final boolean[] stoppedDiscord = new boolean[copiedList.size()];

        int botIndex = 0;
        for (Bot bot : copiedList) {
            try {
                if (discordEnabled) {
                    final String channelId = bot.discord.servers.get(bot.host + ":" + bot.port);

                    final MessageCreateAction messageAction = bot.discord.sendMessageInstantly(stoppingMessage, channelId, false);

                    final int finalBotIndex = botIndex;
                    messageAction.queue(
                            (message) -> stoppedDiscord[finalBotIndex] = true,
                            (error) -> stoppedDiscord[finalBotIndex] = true // should i also set this to true on fail?
                    );
                }

                if (ircEnabled) bot.irc.quit(stoppingMessage);

                bot.stop();
            } catch (Exception ignored) {}

            botIndex++;
        }

        if (discordEnabled) {
            discord.jda.shutdown();

            for (int i = 0; i < 150; i++) {
                try {
                    if (!ArrayUtilities.isAllTrue(stoppedDiscord)) Thread.sleep(50);
                    else break;
                } catch (InterruptedException ignored) {}
            }
        }

        System.exit(exitCode);
    }
}