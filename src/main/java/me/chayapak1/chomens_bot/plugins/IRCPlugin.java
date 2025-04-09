package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.contexts.IRCCommandContext;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.cap.SASLCapHandler;
import org.pircbotx.delay.StaticReadonlyDelay;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import javax.net.ssl.SSLSocketFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class IRCPlugin extends ListenerAdapter {
    private final Configuration.IRC ircConfig;

    private final Map<String, String> servers;

    public final Map<String, List<String>> messageQueue = new HashMap<>();

    private PircBotX bot;

    public IRCPlugin (final Configuration config) {
        this.ircConfig = config.irc;

        this.servers = ircConfig.servers;

        if (!ircConfig.enabled) return;

        final org.pircbotx.Configuration.Builder builder = new org.pircbotx.Configuration.Builder()
                .setName(ircConfig.name)
                .setLogin("bot@chomens-bot")
                .addServer(ircConfig.host, ircConfig.port)
                .setSocketFactory(SSLSocketFactory.getDefault())
                .setAutoReconnect(true)
                .setMessageDelay(new StaticReadonlyDelay(50))
                .addListener(this);

        if (!ircConfig.password.isEmpty())
            builder.addCapHandler(new SASLCapHandler(ircConfig.name, ircConfig.password, true));

        for (final Map.Entry<String, String> entry : ircConfig.servers.entrySet())
            builder.addAutoJoinChannel(entry.getValue());

        final org.pircbotx.Configuration configuration = builder.buildConfiguration();

        bot = new PircBotX(configuration);

        new Thread(() -> {
            try {
                bot.startBot();
            } catch (final Exception e) {
                LoggerUtilities.error(e);
            }
        }).start();

        for (final Bot bot : Main.bots) {
            bot.addListener(new Bot.Listener() {
                @Override
                public void connected (final ConnectedEvent event) {
                    IRCPlugin.this.connected(bot);
                }

                @Override
                public void loadedPlugins (final Bot bot) {
                    bot.chat.addListener(new ChatPlugin.Listener() {
                        @Override
                        public boolean systemMessageReceived (final Component component, final String string, final String ansi) {
                            IRCPlugin.this.systemMessageReceived(bot, ansi);

                            return true;
                        }
                    });
                }
            });
        }

        Main.EXECUTOR.scheduleAtFixedRate(this::queueTick, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onMessage (final MessageEvent event) {
        Bot serverBot = null;

        String targetChannel = null;

        for (final Map.Entry<String, String> entry : servers.entrySet()) {
            if (entry.getValue().equals(event.getChannel().getName())) {
                serverBot = Main.bots.stream()
                        .filter(eachBot -> entry.getKey().equals(eachBot.getServerString(true)))
                        .findFirst()
                        .orElse(null);

                targetChannel = entry.getValue();

                break;
            }
        }

        if (serverBot == null) return;

        final String commandPrefix = ircConfig.prefix;

        final User user = event.getUser();

        if (user == null) return;

        final String name = user.getRealName().isBlank() ? user.getNick() : user.getRealName();
        final String message = event.getMessage();

        if (message.startsWith(commandPrefix)) {
            final String noPrefix = message.substring(commandPrefix.length());

            final IRCCommandContext context = new IRCCommandContext(serverBot, commandPrefix, name);

            serverBot.commandHandler.executeCommand(noPrefix, context, null);

            return;
        }

        final Component prefix = Component
                .text(targetChannel)
                .hoverEvent(
                        HoverEvent.showText(
                                Component
                                        .empty()
                                        .append(Component.text("on "))
                                        .append(Component.text(ircConfig.host))
                                        .append(Component.text(":"))
                                        .append(Component.text(ircConfig.port))
                                        .color(NamedTextColor.GRAY)
                        )
                )
                .color(NamedTextColor.BLUE);

        final Component username = Component
                .text(name)
                .hoverEvent(HoverEvent.showText(Component.text(event.getUser().getHostname()).color(NamedTextColor.RED)))
                .color(NamedTextColor.RED);

        final Component messageComponent = Component
                .text(message)
                .color(NamedTextColor.GRAY);

        final Component component = Component.translatable(
                "[%s] %s › %s",
                prefix,
                username,
                messageComponent
        ).color(NamedTextColor.DARK_GRAY);

        serverBot.chat.tellraw(component);
    }

    private void systemMessageReceived (final Bot bot, final String ansi) {
        sendMessage(
                bot,
                ansi
        );
    }

    public void quit (final String reason) {
        if (bot.isConnected()) bot.sendIRC().quitServer(reason);
    }

    private void connected (final Bot bot) {
        sendMessage(
                bot,
                String.format(
                        "Successfully connected to: %s",
                        bot.getServerString()
                )
        );
    }

    private void queueTick () {
        if (!bot.isConnected() || messageQueue.isEmpty()) return;

        try {
            final Map<String, List<String>> clonedMap = new HashMap<>(messageQueue);

            for (final Map.Entry<String, List<String>> entry : clonedMap.entrySet()) {
                final List<String> logs = entry.getValue();

                if (logs.isEmpty()) continue;

                final String firstLog = logs.getFirst();

                logs.removeFirst();

                final String withIRCColors = ColorUtilities.convertAnsiToIrc(firstLog);

                bot.sendIRC().message(entry.getKey(), withIRCColors);
            }
        } catch (final Exception ignored) { }
    }

    private void addMessageToQueue (final Bot bot, final String message) {
        final String channel = servers.get(bot.getServerString(true));

        addMessageToQueue(channel, message);
    }

    private void addMessageToQueue (final String channel, final String message) {
        final List<String> split = new ArrayList<>(Arrays.asList(message.split("\n")));

        if (!messageQueue.containsKey(channel)) {
            messageQueue.put(channel, split);
        } else {
            if (messageQueue.get(channel).size() > 10) return;

            messageQueue.get(channel).addAll(split);
        }
    }

    public void sendMessage (final Bot bot, final String message) {
        final String hostAndPort = bot.getServerString(true);

        final String channel = servers.get(hostAndPort);

        if (channel == null) return;

        addMessageToQueue(channel, message);
    }
}
