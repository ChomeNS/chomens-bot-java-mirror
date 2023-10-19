package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.IRCCommandContext;
import land.chipmunk.chayapak.chomens_bot.data.IRCMessage;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerMessage;
import land.chipmunk.chayapak.chomens_bot.irc.IRCMessageLoop;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class IRCPlugin extends IRCMessageLoop {
    private final Configuration.IRC ircConfig;

    private final Map<String, String> servers;

    private final Map<String, List<String>> messageQueue = new HashMap<>();

    private String lastMessage = "";

    public IRCPlugin (Configuration config) {
        super(config.irc.host, config.irc.port);

        this.ircConfig = config.irc;

        this.servers = ircConfig.servers;

        nick(ircConfig.nickname);
        user(ircConfig.username, ircConfig.hostName, ircConfig.serverName, ircConfig.realName);

        for (Map.Entry<String, String> entry : ircConfig.servers.entrySet()) join(entry.getValue());

        Main.executorService.submit(this);

        for (Bot bot : Main.bots) {
            bot.addListener(new Bot.Listener() {
                @Override
                public void connected(ConnectedEvent event) {
                    IRCPlugin.this.connected(bot);
                }

                @Override
                public void loadedPlugins() {
                    bot.chat.addListener(new ChatPlugin.Listener() {
                        @Override
                        public void playerMessageReceived(PlayerMessage message) {
                            IRCPlugin.this.playerMessageReceived(bot, message);
                        }
                    });
                }
            });

            bot.irc = this;
        }

        Main.executor.scheduleAtFixedRate(this::queueTick, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onMessage (IRCMessage message) {
        Bot serverBot = null;

        String channel = null;

        for (Map.Entry<String, String> entry : servers.entrySet()) {
            if (entry.getValue().equals(message.channel)) {
                serverBot = Main.bots.stream()
                        .filter(eachBot -> entry.getKey().equals(eachBot.host + ":" + eachBot.port))
                        .toArray(Bot[]::new)[0];

                channel = entry.getValue();

                break;
            }
        }

        if (serverBot == null) return;

        final String commandPrefix = ircConfig.prefix;

        if (message.content.startsWith(commandPrefix)) {
            final String noPrefix = message.content.substring(commandPrefix.length());

            final IRCCommandContext context = new IRCCommandContext(serverBot, commandPrefix, message.nickName);

            final Component output = serverBot.commandHandler.executeCommand(noPrefix, context, null);

            if (output != null) context.sendOutput(output);

            return;
        }

        final Component prefix = Component
                .text(channel)
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
                .text(message.nickName)
                .hoverEvent(HoverEvent.showText(Component.text(message.origin).color(NamedTextColor.RED)))
                .color(NamedTextColor.RED);

        final Component messageComponent = Component
                .text(message.content)
                .color(NamedTextColor.GRAY);

        final Component component = Component.translatable(
                "[%s] %s › %s",
                prefix,
                username,
                messageComponent
        ).color(NamedTextColor.DARK_GRAY);

        serverBot.chat.tellraw(component);
    }

    private void playerMessageReceived (Bot bot, PlayerMessage message) {
        final String stringifiedName = ComponentUtilities.stringify(message.displayName);
        final String stringifiedContents = ComponentUtilities.stringify(message.contents);

        final String toSend = String.format(
                "<%s> %s",
                stringifiedName,
                stringifiedContents
        );

        if (lastMessage.equals(toSend)) return;

        lastMessage = toSend;

        addMessageToQueue(
                bot,
                toSend
        );
    }

    // i only have listened to connected because the ratelimit
    private void connected (Bot bot) {
        sendMessage(
                bot,
                String.format(
                        "Successfully connected to: %s:%s",
                        bot.host,
                        bot.port
                )
        );
    }

    private void queueTick () {
        if (!initialSetupStatus || messageQueue.isEmpty()) return;

        for (Map.Entry<String, List<String>> entry : messageQueue.entrySet()) {
            final List<String> logs = entry.getValue();

            if (logs.isEmpty()) continue;

            final String firstLog = logs.get(0);

            logs.remove(0);

            channelMessage(entry.getKey(), firstLog);
        }
    }

    private void addMessageToQueue (Bot bot, String message) {
        final String channel = servers.get(bot.host + ":" + bot.port);

        addMessageToQueue(channel, message);
    }

    private void addMessageToQueue (String channel, String message) {
        final List<String> split = new ArrayList<>(Arrays.asList(message.split("\n")));

        if (!messageQueue.containsKey(channel)) {
            messageQueue.put(channel, split);
        } else {
            if (messageQueue.get(channel).size() > 10) return;

            messageQueue.get(channel).addAll(split);
        }
    }

    public void sendMessage (Bot bot, String message) {
        final String hostAndPort = bot.host + ":" + bot.port;

        final String channel = servers.get(hostAndPort);

        if (channel == null) {
            bot.logger.error("Channel is not configured for " + hostAndPort);
            return;
        }

        addMessageToQueue(channel, message);
    }
}
