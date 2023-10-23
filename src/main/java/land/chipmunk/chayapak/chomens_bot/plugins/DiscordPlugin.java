package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.DiscordCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.CodeBlockUtilities;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.*;

// please ignore my ohio code
// also this is one of the classes which has >100 lines or actually >300 LMAO
public class DiscordPlugin {
    public final JDA jda;

    public final Map<String, String> servers;

    public final String prefix;

    public Component messagePrefix;

    public final String discordUrl;

    public boolean shuttedDown = false;

    public DiscordPlugin (Configuration config, JDA jda) {
        final Configuration.Discord options = config.discord;
        this.prefix = options.prefix;
        this.servers = options.servers;
        this.jda = jda;
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

        if (jda == null) return;

        for (Bot bot : Main.bots) {
            final String channelId = servers.get(bot.host + ":" + bot.port);

            bot.addListener(new Bot.Listener() {
                @Override
                public void loadedPlugins() {
                    bot.tick.addListener(new TickPlugin.Listener() {
                        @Override
                        public void onAlwaysTick () {
                            onDiscordTick(channelId);
                        }
                    });

                    bot.chat.addListener(new ChatPlugin.Listener() {
                        @Override
                        public boolean systemMessageReceived (Component component, String string, String ansi) {
                            if (string.length() > 2048) {
                                sendMessage(CodeBlockUtilities.escape(string), channelId);
                            } else {
                                sendMessage(
                                        CodeBlockUtilities.escape(
                                                ansi
                                                        .replace(
                                                                "\u001b[9", "\u001b[3"
                                                        )
                                        ),
                                        channelId
                                );
                            }

                            return true;
                        }
                    });
                }

                @Override
                public void connecting() {
                    sendMessageInstantly(
                            String.format(
                                    "Connecting to: `%s:%s`",
                                    bot.host,
                                    bot.port
                            ),
                            channelId
                    );
                }

                @Override
                public void connected (ConnectedEvent event) {
                    sendMessageInstantly(
                            String.format(
                                    "Successfully connected to: `%s:%s`",
                                    bot.host,
                                    bot.port
                            ),
                            channelId
                    );
                }

                @Override
                public void disconnected(DisconnectedEvent event) {
                    final String reason = ComponentUtilities.stringifyAnsi(event.getReason());
                    sendMessageInstantly(
                            "Disconnected: \n" +
                                    "```ansi\n" +
                                    reason.replace("`", "\\`") +
                                    "\n```"
                            , channelId
                    );
                }
            });

            jda.addEventListener(new ListenerAdapter() {
                @Override
                public void onMessageReceived(@NotNull MessageReceivedEvent event) {
                    // TODO: IMPROVE this code
                    if (
                            !event.getChannel().getId().equals(channelId) ||
                                    event.getAuthor().getId().equals(jda.getSelfUser().getId()) ||
                                    !bot.loggedIn
                    ) return;

                    final Message messageEvent = event.getMessage();
                    final String message = messageEvent.getContentDisplay();

                    if (message.startsWith(prefix)) {
                        final DiscordCommandContext context = new DiscordCommandContext(bot, prefix, event);

                        final Component output = bot.commandHandler.executeCommand(message.substring(prefix.length()), context, event);

                        if (output != null) {
                            context.sendOutput(output);
                        }

                        return;
                    }

                    // ignore my very very ohio code,..,,.

                    Component attachmentsComponent = Component.empty();
                    if (messageEvent.getAttachments().size() > 0) {
                        attachmentsComponent = attachmentsComponent.append(Component.space());
                        for (Message.Attachment attachment : messageEvent.getAttachments()) {
                            attachmentsComponent = attachmentsComponent
                                    .append(
                                            Component
                                                    .text("[Attachment]")
                                                    .clickEvent(ClickEvent.openUrl(attachment.getProxyUrl()))
                                                    .color(NamedTextColor.GREEN)
                                    )
                                    .append(Component.space());
                        }
                    }

                    Component embedsComponent = Component.empty();
                    if (messageEvent.getEmbeds().size() > 0) {
                        if (messageEvent.getAttachments().isEmpty()) embedsComponent = embedsComponent.append(Component.space());
                        for (MessageEmbed embed : messageEvent.getEmbeds()) {
                            final Component hoverEvent = Component.translatable(
                                    """
                                            Title: %s
                                            %s""",
                                    embed.getTitle() == null ?
                                            Component.text("No title").color(NamedTextColor.GRAY) :
                                            Component.text(embed.getTitle()).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                                    embed.getDescription() == null ?
                                            Component.text("No description").color(NamedTextColor.GRAY) :
                                            Component.text(embed.getDescription()).color(NamedTextColor.WHITE)
                            ).color(NamedTextColor.GREEN);

                            embedsComponent = embedsComponent
                                    .append(
                                            Component
                                                    .text("[Embed]")
                                                    .hoverEvent(HoverEvent.showText(hoverEvent))
                                                    .color(NamedTextColor.GREEN)
                                    )
                                    .append(Component.space());
                        }
                    }

                    final Member member = event.getMember();

                    final String username = event.getAuthor().getName();
                    final String displayName = member == null ? username : member.getEffectiveName();

                    final List<Role> roles = member == null ? Collections.emptyList() : member.getRoles();

                    Component rolesComponent = Component.empty();
                    if (roles.size() > 0) {
                        rolesComponent = rolesComponent
                                .append(Component.text("Roles:").color(NamedTextColor.GRAY))
                                .append(Component.newline());

                        final List<Component> rolesList = new ArrayList<>();

                        for (Role role : roles) {
                            final Color color = role.getColor();

                            rolesList.add(
                                    Component
                                            .text(role.getName())
                                            .color(
                                                    color == null ?
                                                            NamedTextColor.WHITE :
                                                            TextColor.color(
                                                                    color.getRed(),
                                                                    color.getGreen(),
                                                                    color.getBlue()
                                                            )
                                            )
                            );
                        }

                        rolesComponent = rolesComponent.append(Component.join(JoinConfiguration.newlines(), rolesList));
                    } else {
                        rolesComponent = rolesComponent.append(Component.text("No roles").color(NamedTextColor.GRAY));
                    }

                    Component nameComponent = Component
                            .text(displayName)
                            .clickEvent(ClickEvent.copyToClipboard(username))
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.translatable(
                                                    """
                                                            %s
                                                            %s
                                                            
                                                            %s""",
                                                    Component.text(username).color(NamedTextColor.WHITE),
                                                    rolesComponent,
                                                    Component.text("Click here to copy the tag to your clipboard").color(NamedTextColor.GREEN)
                                            ).color(NamedTextColor.DARK_GRAY)
                                    )
                            );

                    // too ohio
                    for (Role role : roles) {
                        final Color color = role.getColor();

                        if (color == null) continue;

                        nameComponent = nameComponent.color(
                                TextColor.color(
                                        color.getRed(),
                                        color.getGreen(),
                                        color.getBlue()
                                )
                        );

                        break;
                    }

                    if (nameComponent.color() == null) nameComponent = nameComponent.color(NamedTextColor.RED);

                    final Component deserialized = LegacyComponentSerializer.legacyAmpersand().deserialize(message.replace("\uD83D\uDC80", "☠"));

                    final Component messageComponent = Component
                            .text("")
                            .color(NamedTextColor.GRAY)
                            .append(
                                    deserialized
                                            .append(attachmentsComponent)
                                            .append(embedsComponent)
                            );

                    final Component component = Component.translatable(
                            "[%s] %s › %s",
                            messagePrefix,
                            nameComponent,
                            messageComponent
                    ).color(NamedTextColor.DARK_GRAY);

                    bot.chat.tellraw(component);
                }

                @Override
                public void onShutdown(@NotNull ShutdownEvent event) {
                    shuttedDown = true;
                }
            });

            bot.discord = this;
        }
    }

    // totallynotskidded™️ from HBot (and changed a bit)
    final Map<String, StringBuilder> logMessages = new HashMap<>();
    final Map<String, Long> nextLogTimes = new HashMap<>();
    final Map<String, Boolean> doneSendingInLogs = new HashMap<>();

    public void sendMessage(String message, String channelId) {
        synchronized (logMessages) {
            if (!logMessages.containsKey(channelId)) {
                logMessages.put(channelId, new StringBuilder());
            }
            StringBuilder logMessage = logMessages.get(channelId);
            if (logMessage.length() < 2000) {
                if (!logMessage.isEmpty()) {
                    logMessage.append('\n');
                }
                logMessage.append(message);
            }
        }
    }

    public void sendMessageInstantly (String message, String channelId) {
        if (jda == null) return;

        final TextChannel logChannel = jda.getTextChannelById(channelId);

        if (logChannel == null) {
            LoggerUtilities.error("Log channel for " + channelId + " is null");
            return;
        }

        logChannel.sendMessage(message).queue(
                (msg) -> doneSendingInLogs.put(channelId, true),
                (err) -> {
                    err.printStackTrace();

                    doneSendingInLogs.put(channelId, false);
                }
        );
    }

    public void onDiscordTick(String channelId) {
        synchronized (logMessages) {
            if (!logMessages.containsKey(channelId) || logMessages.get(channelId).length() == 0) {
                return;
            }
        }

        long currentTime = System.currentTimeMillis();
        if (!nextLogTimes.containsKey(channelId) || (currentTime >= nextLogTimes.get(channelId) && doneSendingInLogs.get(channelId))
                || currentTime - nextLogTimes.get(channelId) > 5000) {
            long logDelay = 2000;

            nextLogTimes.put(channelId, currentTime + logDelay);
            String message;
            synchronized (logMessages) {
                StringBuilder logMessage = logMessages.get(channelId);
                message = logMessage.toString();
                final int maxLength = 2_000 - ("""
                        ```ansi

                        ```"""
                ).length(); // kinda sus
                if (message.length() >= maxLength) {
                    message = message.substring(0, maxLength);
                }
                logMessage.setLength(0);
            }

            if (message.trim().isBlank()) return;

            sendMessageInstantly("```ansi\n" + message + "\n```", channelId);
        }
    }
}
