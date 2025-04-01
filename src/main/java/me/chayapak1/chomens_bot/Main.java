package me.chayapak1.chomens_bot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.chayapak1.chomens_bot.plugins.*;
import me.chayapak1.chomens_bot.util.ArrayUtilities;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Path stopReasonFilePath = Path.of("shutdown_reason.txt");

    public static final List<Bot> bots = new ArrayList<>();

    public static final ExecutorService executorService = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            new ThreadFactoryBuilder().setNameFormat("ExecutorService #%d").build()
    );
    public static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            new ThreadFactoryBuilder().setNameFormat("ScheduledExecutorService #%d").build()
    );

    private static Configuration config;

    private static boolean alreadyStarted = false;

    private static boolean stopping = false;

    private static int backupFailTimes = 0;

    public static ConsolePlugin console;
    public static DatabasePlugin database;
    private static DiscordPlugin discord;

    public static void main (String[] args) throws IOException {
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

            LoggerUtilities.log("config.yml file was not found, so the default one was created. Please modify it to your needs.");

            System.exit(1);
        }

        InputStream opt = Files.newInputStream(configPath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);

        final Thread shutdownThread = new Thread(Main::handleShutdown);
        shutdownThread.setName("ChomeNS Bot Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        if (!config.backup.enabled) {
            initializeBots();
        } else {
            executor.scheduleAtFixedRate(() -> {
                boolean reachable;

                try {
                    HttpUtilities.getRequest(new URI(config.backup.address).toURL());

                    reachable = true;
                } catch (Exception e) {
                    reachable = false;
                }

                if (!reachable && !alreadyStarted) {
                    backupFailTimes++;

                    if (backupFailTimes > config.backup.failTimes) {
                        LoggerUtilities.log("Main instance is down! Starting backup instance");

                        initializeBots();
                    }
                } else if (reachable && alreadyStarted) {
                    LoggerUtilities.log("Main instance is back up! Now stopping");

                    // no need to reset backupFailTimes because we are stopping anyway

                    stop(0);
                }
            }, 0, config.backup.interval, TimeUnit.MILLISECONDS);
        }
    }

    public static void initializeBots () {
        alreadyStarted = true;

        try {
            final Configuration.BotOption[] botsOptions = config.bots;

            for (Configuration.BotOption botOption : botsOptions) {
                final Bot bot = new Bot(botOption, bots, config);
                bots.add(bot);
            }

            // initialize plugins
            console = new ConsolePlugin(config);
            LoggerPlugin.init();
            ChomeNSModIntegrationPlugin.init();
            if (config.database.enabled) database = new DatabasePlugin(config);
            if (config.discord.enabled) discord = new DiscordPlugin(config);
            if (config.irc.enabled) new IRCPlugin(config);

            LoggerUtilities.log("Initialized all bots. Now connecting");

            for (Bot bot : bots) bot.connect();
        } catch (Exception e) {
            LoggerUtilities.error(e);

            System.exit(1);
        }
    }

    private static void handleShutdown () {
        String reason = null;

        if (Files.exists(stopReasonFilePath)) {
            try {
                reason = new String(Files.readAllBytes(stopReasonFilePath)).trim();
            } catch (IOException ignored) { }
        }

        stop(0, reason, false);
    }

    // most of these are stolen from HBot
    public static void stop (int exitCode) { stop(exitCode, null, null, true); }

    public static void stop (int exitCode, String reason) { stop(exitCode, reason, null, true); }

    public static void stop (int exitCode, String reason, String type) { stop(exitCode, reason, type, true); }

    public static void stop (int exitCode, String reason, boolean callSystemExit) { stop(exitCode, reason, null, callSystemExit); }

    public static void stop (int exitCode, String reason, String type, boolean callSystemExit) {
        if (stopping) return;

        stopping = true;

        final String stoppingMessage = String.format(
                "%s (%s)",
                type != null ? type : "Stopping..",
                reason != null ? reason : "No reason given"
        );

        LoggerUtilities.log(stoppingMessage);

        executor.shutdown();

        executorService.shutdown();

        if (database != null) database.stop();

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
                    final String channelId = bot.discord.servers.get(bot.getServerString(true));

                    final MessageCreateAction messageAction = bot.discord.sendMessageInstantly(stoppingMessage, channelId, false);

                    final int finalBotIndex = botIndex;
                    messageAction.queue(
                            (message) -> stoppedDiscord[finalBotIndex] = true,
                            (error) -> stoppedDiscord[finalBotIndex] = true // should i also set this to true on fail?
                    );
                }

                if (ircEnabled) bot.irc.quit(stoppingMessage);

                bot.stop();
            } catch (Exception ignored) { }

            botIndex++;
        }

        if (discordEnabled) {
            for (int i = 0; i < 150; i++) {
                try {
                    if (!ArrayUtilities.isAllTrue(stoppedDiscord)) Thread.sleep(50);
                    else break;
                } catch (InterruptedException ignored) { }
            }

            discord.jda.shutdown();
        }

        if (callSystemExit) System.exit(exitCode);
    }
}