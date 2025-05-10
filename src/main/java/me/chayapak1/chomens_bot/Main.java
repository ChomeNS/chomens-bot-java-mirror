package me.chayapak1.chomens_bot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.plugins.ConsolePlugin;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.plugins.DiscordPlugin;
import me.chayapak1.chomens_bot.plugins.IRCPlugin;
import me.chayapak1.chomens_bot.util.ArrayUtilities;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import me.chayapak1.chomens_bot.util.I18nUtilities;
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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    public static final Path stopReasonFilePath = Path.of("shutdown_reason.txt");

    public static final List<Bot> bots = new ObjectArrayList<>();

    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            new ThreadFactoryBuilder().setNameFormat("ExecutorService #%d").build()
    );
    public static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(
            Math.max(1, Runtime.getRuntime().availableProcessors() / 2),
            new ThreadFactoryBuilder().setNameFormat("ScheduledExecutorService #%d").build()
    );

    private static Configuration config;

    private static boolean alreadyStarted = false;

    public static boolean stopping = false;

    private static int backupFailTimes = 0;

    public static ConsolePlugin console;
    public static DatabasePlugin database;
    public static DiscordPlugin discord;
    public static IRCPlugin irc;

    public static void main (final String[] args) throws IOException {
        Locale.setDefault(Locale.ROOT);

        loadConfig();

        final Thread shutdownThread = new Thread(Main::handleShutdown, "ChomeNS Bot Shutdown Thread");
        Runtime.getRuntime().addShutdownHook(shutdownThread);

        if (!config.backup.enabled) {
            initializeBots();
        } else {
            EXECUTOR.scheduleAtFixedRate(() -> {
                boolean reachable;

                try {
                    HttpUtilities.getRequest(new URI(config.backup.address).toURL());

                    reachable = true;
                } catch (final Exception e) {
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

    private static void loadConfig () throws IOException {
        final Path configPath = Path.of("config.yml");

        final Constructor constructor = new Constructor(Configuration.class, new LoaderOptions());
        final Yaml yaml = new Yaml(constructor);

        if (!Files.exists(configPath)) {
            // creates config file from default-config.yml
            final InputStream is = ClassLoader.getSystemClassLoader().getResourceAsStream("default-config.yml");

            if (is == null) System.exit(1);

            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final StringBuilder stringBuilder = new StringBuilder();
            while (reader.ready()) {
                final char character = (char) reader.read();
                stringBuilder.append(character);
            }
            final String defaultConfig = stringBuilder.toString();

            // writes it
            final BufferedWriter configWriter = Files.newBufferedWriter(configPath);
            configWriter.write(defaultConfig);
            configWriter.close();

            LoggerUtilities.log("config.yml file was not found, so the default one was created. Please modify it to your needs.");

            System.exit(1);
        }

        final InputStream opt = Files.newInputStream(configPath);
        final BufferedReader reader = new BufferedReader(new InputStreamReader(opt));

        config = yaml.load(reader);
    }

    private static void initializeBots () {
        alreadyStarted = true;

        try {
            if (config.database.enabled) database = new DatabasePlugin(config);

            final Configuration.BotOption[] botsOptions = config.bots;

            for (final Configuration.BotOption botOption : botsOptions) {
                final Bot bot = new Bot(botOption, bots, config);
                bots.add(bot);
            }

            console = new ConsolePlugin(config);
            if (config.discord.enabled) discord = new DiscordPlugin(config);
            if (config.irc.enabled) irc = new IRCPlugin(config);

            LoggerUtilities.log(I18nUtilities.get("initialized"));

            for (final Bot bot : bots) bot.connect();
        } catch (final Exception e) {
            LoggerUtilities.error(e);

            System.exit(1);
        }
    }

    private static void handleShutdown () {
        String reason = null;

        if (Files.exists(stopReasonFilePath)) {
            try {
                reason = new String(Files.readAllBytes(stopReasonFilePath)).trim();
            } catch (final IOException ignored) { }
        }

        stop(0, reason, false);
    }

    // most of these are stolen from HBot
    public static void stop (final int exitCode) { stop(exitCode, null, null, true); }

    public static void stop (final int exitCode, final String reason) { stop(exitCode, reason, null, true); }

    public static void stop (final int exitCode, final String reason, final String type) { stop(exitCode, reason, type, true); }

    public static void stop (final int exitCode, final String reason, final boolean callSystemExit) { stop(exitCode, reason, null, callSystemExit); }

    public static void stop (final int exitCode, final String reason, final String type, final boolean callSystemExit) {
        if (stopping) return;
        else stopping = true;

        final String stoppingMessage = String.format(
                I18nUtilities.get("info.stopping.generic"),
                type != null ? type : I18nUtilities.get("info.stopping"),
                reason != null ? reason : I18nUtilities.get("info.no_reason")
        );

        LoggerUtilities.log(stoppingMessage);

        EXECUTOR.shutdown();
        EXECUTOR_SERVICE.shutdown();
        if (database != null) database.stop();

        final ArrayList<Bot> copiedList;
        synchronized (bots) {
            copiedList = new ArrayList<>(bots);
        }

        final boolean ircEnabled = config.irc.enabled;
        final boolean discordEnabled = config.discord.enabled;

        if (ircEnabled) irc.quit(stoppingMessage);

        final boolean[] stoppedDiscord = new boolean[copiedList.size()];

        int botIndex = 0;
        for (final Bot bot : copiedList) {
            try {
                if (discordEnabled) {
                    final String channelId = Main.discord.servers.get(bot.getServerString(true));

                    final MessageCreateAction messageAction = Main.discord.sendMessageInstantly(stoppingMessage, channelId, false);

                    final int finalBotIndex = botIndex;
                    messageAction.queue(
                            (message) -> stoppedDiscord[finalBotIndex] = true,
                            (error) -> stoppedDiscord[finalBotIndex] = true // should i also set this to true on fail?
                    );
                }

                bot.stop();
            } catch (final Exception ignored) { }

            botIndex++;
        }

        if (discordEnabled) {
            for (int i = 0; i < 150; i++) {
                try {
                    if (!ArrayUtilities.isAllTrue(stoppedDiscord)) Thread.sleep(50);
                    else break;
                } catch (final InterruptedException ignored) { }
            }

            if (discord != null && discord.jda != null) discord.jda.shutdown();
        }

        if (callSystemExit) System.exit(exitCode);
    }
}