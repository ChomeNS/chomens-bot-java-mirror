package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.discord.DirectMessageEventHandler;
import me.chayapak1.chomens_bot.discord.GuildMessageEventHandler;
import me.chayapak1.chomens_bot.discord.MessageLogger;
import me.chayapak1.chomens_bot.util.CodeBlockUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class DiscordPlugin {
    public JDA jda;

    public Configuration.Discord options;

    public final Map<String, String> servers;

    public final String prefix;

    public final Component messagePrefix;

    public final String discordUrl;

    public DiscordPlugin (final Configuration config) {
        this.options = config.discord;
        this.prefix = options.prefix;
        this.servers = options.servers;
        this.discordUrl = config.discord.inviteLink;
        this.messagePrefix = Component.empty()
                .append(Component.text("ChomeNS ").color(NamedTextColor.YELLOW))
                .append(Component.text("Discord").color(NamedTextColor.BLUE))
                .hoverEvent(
                        HoverEvent.showText(
                                Component.text("Click here to join the Discord server").color(NamedTextColor.GREEN)
                        )
                )
                .clickEvent(ClickEvent.openUrl(discordUrl));

        final JDABuilder builder = JDABuilder.createDefault(config.discord.token);
        builder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.GUILD_MEMBERS
        );
        builder.setEnableShutdownHook(false);

        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (final InterruptedException ignored) { }

        if (jda == null) return;

        jda.getPresence().setPresence(Activity.playing(config.discord.statusMessage), false);

        new MessageLogger(jda);
        new GuildMessageEventHandler(jda, prefix, messagePrefix);
        new DirectMessageEventHandler(jda, options);

        Main.EXECUTOR.scheduleAtFixedRate(this::onDiscordTick, 0, 50, TimeUnit.MILLISECONDS);

        for (final Bot bot : Main.bots) {
            final String channelId = servers.get(bot.getServerString(true));

            logData.put(channelId, new LogData());

            bot.addListener(new Bot.Listener() {
                @Override
                public void loadedPlugins (final Bot bot) {
                    bot.chat.addListener(new ChatPlugin.Listener() {
                        @Override
                        public boolean systemMessageReceived (final Component component, final String string, final String _ansi) {
                            if (string.length() > 2000 - 12) {
                                sendMessage(CodeBlockUtilities.escape(string), channelId);
                            } else {
                                final String ansi = ComponentUtilities.stringifyDiscordAnsi(component);

                                sendMessage(
                                        CodeBlockUtilities.escape(ansi),
                                        channelId
                                );
                            }

                            return true;
                        }
                    });
                }

                @Override
                public void connecting () {
                    if (bot.connectAttempts > 6) return;
                    else if (bot.connectAttempts == 6) {
                        sendMessageInstantly("Suppressing connection status messages from now on", channelId);

                        return;
                    }

                    sendMessageInstantly(
                            String.format(
                                    "Connecting to: `%s`",
                                    bot.getServerString()
                            ),
                            channelId
                    );
                }

                @Override
                public void connected (final ConnectedEvent event) {
                    sendMessageInstantly(
                            String.format(
                                    "Successfully connected to: `%s`",
                                    bot.getServerString()
                            ),
                            channelId
                    );
                }

                @Override
                public void disconnected (final DisconnectedEvent event) {
                    if (bot.connectAttempts >= 6) return;

                    final String reason = ComponentUtilities.stringifyDiscordAnsi(event.getReason());
                    sendMessageInstantly(
                            "Disconnected: \n" +
                                    "```ansi\n" +
                                    reason.replace("`", "\\`") +
                                    "\n```",
                            channelId
                    );
                }
            });
        }
    }

    // based from HBot (and modified quite a bit)
    private final Map<String, LogData> logData = new ConcurrentHashMap<>();

    public void sendMessage (final String message, final String channelId) {
        final LogData data = logData.get(channelId);

        if (data == null) return;

        final StringBuilder logMessage = data.logMessages;

        if (logMessage.length() < 2000) {
            if (!logMessage.isEmpty()) {
                logMessage.append('\n');
            }
            logMessage.append(message);
        }
    }

    public void sendMessageInstantly (final String message, final String channelId) { sendMessageInstantly(message, channelId, true); }

    public MessageCreateAction sendMessageInstantly (final String message, final String channelId, final boolean queue) {
        if (jda == null) return null;

        final TextChannel logChannel = jda.getTextChannelById(channelId);

        if (logChannel == null) {
            LoggerUtilities.error("Log channel for " + channelId + " is null");
            return null;
        }

        if (queue) {
            final LogData data = logData.get(channelId);

            if (data == null) return null;

            logChannel.sendMessage(message).queue(
                    (msg) -> data.doneSendingInLog.set(true),
                    (e) -> {
                        LoggerUtilities.error(e);

                        data.doneSendingInLog.set(false);
                    }
            );

            return null;
        } else {
            return logChannel.sendMessage(message);
        }
    }

    public void onDiscordTick () {
        for (final Bot bot : Main.bots) {
            final String channelId = servers.get(bot.getServerString(true));

            final LogData data = logData.get(channelId);

            if (data == null) continue;

            final long currentTime = System.currentTimeMillis();

            if (
                    (currentTime < data.nextLogTime.get() || !data.doneSendingInLog.get())
                            && currentTime - data.nextLogTime.get() < (5 * 1000)
            ) continue;

            final long logDelay = 2000;

            data.nextLogTime.set(currentTime + logDelay);

            String message;

            final StringBuilder logMessages = data.logMessages;

            final Matcher inviteMatcher = Message.INVITE_PATTERN.matcher(logMessages.toString());

            final StringBuilder messageBuilder = new StringBuilder();

            while (inviteMatcher.find()) {
                inviteMatcher.appendReplacement(
                        messageBuilder,
                        Matcher.quoteReplacement(
                                inviteMatcher.group()
                                        // fixes discord.gg (and some more discord urls) showing invite
                                        .replace(".", "\u200b.")
                        )
                );
            }

            inviteMatcher.appendTail(messageBuilder);

            final int maxLength = 2_000 - ("""
                    ```ansi
                    
                    ```"""
            ).length(); // kinda sus

            message = messageBuilder.substring(0, Math.min(messageBuilder.length(), maxLength));

            logMessages.setLength(0);

            if (message.trim().isBlank()) continue;

            sendMessageInstantly("```ansi\n" + message + "\n```", channelId);
        }
    }

    private static class LogData {
        private final StringBuilder logMessages = new StringBuilder();
        private final AtomicLong nextLogTime = new AtomicLong(0L);
        private final AtomicBoolean doneSendingInLog = new AtomicBoolean(false);
    }
}
