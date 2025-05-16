package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.discord.DirectMessageEventHandler;
import me.chayapak1.chomens_bot.discord.GuildMessageEventHandler;
import me.chayapak1.chomens_bot.discord.MessageLogger;
import me.chayapak1.chomens_bot.util.CodeBlockUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class DiscordPlugin {
    private static final int MAX_ANSI_MESSAGE_LENGTH = 2_000 - ("""
            ```ansi
            
            ```"""
    ).length(); // kinda sus

    private static final int LOG_DELAY = 2 * 1000;

    public JDA jda;

    public final Configuration.Discord options;

    public final String prefix;
    public final Component messagePrefix;
    public final String serverId;
    public final String discordUrl;

    public DiscordPlugin (final Configuration config) {
        this.options = config.discord;
        this.prefix = options.prefix;
        this.serverId = config.discord.serverId;
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
            final String channelId = findChannelId(bot.options.discordChannel);

            if (channelId == null) continue;

            logData.put(channelId, new LogData());

            bot.listener.addListener(new Listener() {
                @Override
                public boolean onSystemMessageReceived (final Component component, final String string, final String _ansi) {
                    if (string.length() > MAX_ANSI_MESSAGE_LENGTH) {
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

                @Override
                public void onConnecting () {
                    if (!bot.options.logConnectionStatusMessages || bot.connectAttempts > 6) return;
                    else if (bot.connectAttempts == 6) {
                        sendMessageInstantly(I18nUtilities.get("info.suppressing"), channelId);

                        return;
                    }

                    sendMessageInstantly(
                            String.format(
                                    I18nUtilities.get("info.connecting"),
                                    "`" + bot.getServerString().replace("`", "\\`") + "`"
                            ),
                            channelId
                    );
                }

                @Override
                public void connected (final ConnectedEvent event) {
                    sendMessageInstantly(
                            String.format(
                                    I18nUtilities.get("info.connected"),
                                    "`" + bot.getServerString().replace("`", "\\`") + "`"
                            ),
                            channelId
                    );
                }

                @Override
                public void disconnected (final DisconnectedEvent event) {
                    if (!bot.options.logConnectionStatusMessages || bot.connectAttempts >= 6) return;

                    final String reason = ComponentUtilities.stringifyDiscordAnsi(event.getReason());
                    sendMessageInstantly(
                            String.format(
                                    I18nUtilities.get("info.disconnected"),
                                    "\n" +
                                            "```ansi\n" +
                                            CodeBlockUtilities.escape(reason) +
                                            "\n```"
                            ),
                            channelId
                    );
                }
            });
        }
    }

    public @Nullable String findChannelId (final String channelName) {
        final Guild guild = jda.getGuildById(serverId);
        if (guild == null) return null;

        final List<TextChannel> channels = guild.getTextChannelsByName(channelName, true);
        if (channels.isEmpty()) return null;

        return channels.getFirst().getId();
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
            final String channelId = findChannelId(bot.options.discordChannel);

            if (channelId == null) continue;

            final LogData data = logData.get(channelId);

            if (data == null) continue;

            final long currentTime = System.currentTimeMillis();

            if (
                    (currentTime < data.nextLogTime.get() || !data.doneSendingInLog.get())
                            && currentTime - data.nextLogTime.get() < (5 * 1000)
            ) continue;

            data.nextLogTime.set(currentTime + LOG_DELAY);

            final String message;

            final StringBuilder logMessages = data.logMessages;

            final StringBuilder messageBuilder;

            if (logMessages.length() < 4096) { // we obviously don't want to use regex on big strings, or it will take 69 years
                messageBuilder = new StringBuilder();

                final Matcher inviteMatcher = Message.INVITE_PATTERN.matcher(logMessages.toString());

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

                message = messageBuilder.substring(0, Math.min(messageBuilder.length(), MAX_ANSI_MESSAGE_LENGTH));
            } else {
                message = logMessages.toString()
                        .replace(".", "\u200b.")
                        .substring(0, Math.min(logMessages.length(), MAX_ANSI_MESSAGE_LENGTH));
            }

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
