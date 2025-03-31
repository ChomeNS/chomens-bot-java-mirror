package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.contexts.DiscordCommandContext;
import me.chayapak1.chomens_bot.util.CodeBlockUtilities;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

// this is one of the classes which has >500 lines LMAO
public class DiscordPlugin extends ListenerAdapter {
    public JDA jda;

    public final Map<String, String> servers;

    public final String prefix;

    public Component messagePrefix;

    public final String discordUrl;

    private final Map<String, Integer> totalConnects = new HashMap<>();

    public DiscordPlugin (Configuration config) {
        final Configuration.Discord options = config.discord;
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
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.setEnableShutdownHook(false);
        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (InterruptedException ignored) {}

        if (jda == null) return;

        jda.getPresence().setPresence(Activity.playing(config.discord.statusMessage), false);

        jda.addEventListener(this);

        for (Bot bot : Main.bots) {
            final String channelId = servers.get(bot.getServerString(true));

            bot.executor.scheduleAtFixedRate(() -> onDiscordTick(channelId), 0, 50, TimeUnit.MILLISECONDS);

            totalConnects.put(channelId, 0); // is this necessary?

            bot.addListener(new Bot.Listener() {
                @Override
                public void loadedPlugins (Bot bot) {
                    bot.chat.addListener(new ChatPlugin.Listener() {
                        @Override
                        public boolean systemMessageReceived (Component component, String string, String _ansi) {
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
                public void connecting() {
                    final int newTotalConnects = totalConnects.get(channelId) + 1;

                    totalConnects.put(channelId, newTotalConnects);

                    if (newTotalConnects > 6) return;
                    else if (newTotalConnects == 6) {
                        sendMessageInstantly("Suspending connecting and disconnect messages from now on", channelId);

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
                public void connected (ConnectedEvent event) {
                    totalConnects.put(channelId, 0);

                    sendMessageInstantly(
                            String.format(
                                    "Successfully connected to: `%s`",
                                    bot.getServerString()
                            ),
                            channelId
                    );
                }

                @Override
                public void disconnected(DisconnectedEvent event) {
                    if (totalConnects.get(channelId) >= 6) return;

                    final String reason = ComponentUtilities.stringifyAnsi(event.getReason());
                    sendMessageInstantly(
                            "Disconnected: \n" +
                                    "```ansi\n" +
                                    reason.replace("`", "\\`") +
                                    "\n```",
                            channelId
                    );
                }
            });

            bot.discord = this;
        }
    }

    @Override
    public void onMessageReceived (@NotNull MessageReceivedEvent event) {
        for (Bot bot : Main.bots) {
            final String channelId = servers.get(bot.getServerString(true));

            if (
                    !bot.loggedIn ||
                            !event.getChannel().getId().equals(channelId) ||
                            event.getAuthor().getId().equals(jda.getSelfUser().getId())
            ) continue;

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

            final boolean isForwarded = !messageEvent.getMessageSnapshots().isEmpty();

            Component output = Component.empty();

            if (isForwarded) {
                for (MessageSnapshot snapshot : messageEvent.getMessageSnapshots()) {
                    final List<Component> extraComponents = new ArrayList<>();

                    addExtraComponents(
                            extraComponents,
                            snapshot.getAttachments(),
                            snapshot.getEmbeds(),
                            snapshot.getStickers(),
                            bot
                    );

                    Component messageComponent = Component.text(snapshot.getContentRaw());

                    if (!extraComponents.isEmpty()) {
                        messageComponent = messageComponent
                                .append(
                                        Component.join(
                                                JoinConfiguration.spaces(),
                                                extraComponents
                                        )
                                );
                    }

                    output = Component
                            .translatable(
                                    "[%s] %s › %s",
                                    this.messagePrefix,
                                    Component
                                            .translatable(
                                                    "%s forwarded",
                                                    Component.text(
                                                            messageEvent.getMember() == null ?
                                                                    messageEvent.getAuthor().getName() :
                                                                    messageEvent.getMember().getEffectiveName()
                                                    ).color(NamedTextColor.RED)
                                            )
                                            .color(NamedTextColor.GRAY),
                                    messageComponent.color(NamedTextColor.GRAY)
                            )
                            .color(NamedTextColor.DARK_GRAY);
                }
            } else {
                final Message reference = event.getMessage().getReferencedMessage();

                if (reference != null) {
                    output = output
                            .append(
                                    Component.empty()
                                            .append(Component.text("Replying to ").color(NamedTextColor.GRAY))
                                            .append(getMessageComponent(bot, reference))
                                            .decorate(TextDecoration.ITALIC)
                            )
                            .append(Component.newline());
                }

                output = output.append(getMessageComponent(bot, event.getMessage()));
            }

            bot.chat.tellraw(output);
        }
    }

    private Component getMessageComponent (
            Bot bot,
            Message message
    ) {
        final List<Component> extraComponents = new ArrayList<>();

        addExtraComponents(
                extraComponents,
                message.getAttachments(),
                message.getEmbeds(),
                message.getStickers(),
                bot
        );

        final String username = message.getAuthor().getName();
        final Member member = message.getMember();

        final String displayName = member == null ? username : member.getEffectiveName();

        final List<Role> roles = member == null ? Collections.emptyList() : member.getRoles();

        Component rolesComponent = Component.empty();
        if (!roles.isEmpty()) {
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

        final String replacedMessageContent = message.getContentDisplay()
                // replaces skull emoji
                .replace("\uD83D\uDC80", "☠")
                // replaces all ANSI codes (used when a user replies to the bot's log message) with nothing
                .replaceAll("\u001B\\[[;\\d]*[ -/]*[@-~]", "")
                // replaces all ZWSP with nothing
                .replace("\u200b", "");

        Component actualMessage = LegacyComponentSerializer
                .legacyAmpersand()
                .deserialize(replacedMessageContent)
                .replaceText(ComponentUtilities.URL_REPLACEMENT_CONFIG);

        if (!extraComponents.isEmpty()) {
            if (!replacedMessageContent.isBlank()) actualMessage = actualMessage.append(Component.space());

            actualMessage = actualMessage
                    .append(
                            Component.join(
                                    JoinConfiguration.spaces(),
                                    extraComponents
                            )
                    );
        }

        final Component messageComponent = Component.empty()
                .color(NamedTextColor.GRAY)
                .append(actualMessage)
                .hoverEvent(
                        HoverEvent.showText(
                                Component
                                        .text("Click here to copy the message to your clipboard")
                                        .color(NamedTextColor.GREEN)
                        )
                )
                .clickEvent(
                        ClickEvent.copyToClipboard(
                                replacedMessageContent
                        )
                );

        return Component.translatable(
                "[%s] %s › %s",
                messagePrefix,
                nameComponent,
                messageComponent
        ).color(NamedTextColor.DARK_GRAY);
    }

    private void addExtraComponents (
            List<Component> extraComponents,
            List<Message.Attachment> attachments,
            List<MessageEmbed> embeds,
            List<StickerItem> stickers,
            Bot bot
    ) {
        addAttachmentsComponent(attachments, extraComponents);
        addEmbedsComponent(embeds, extraComponents, bot);
        addStickersComponent(stickers, extraComponents);
    }

    private void addAttachmentsComponent (List<Message.Attachment> attachments, List<Component> extraComponents) {
        for (Message.Attachment attachment : attachments) {
            extraComponents.add(
                    Component
                            .text("[Attachment]")
                            .clickEvent(ClickEvent.openUrl(attachment.getUrl()))
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component
                                                    .text(attachment.getFileName())
                                                    .color(NamedTextColor.GREEN)
                                    )
                            )
                            .color(NamedTextColor.GREEN)
            );
        }
    }

    private void addEmbedsComponent (List<MessageEmbed> embeds, List<Component> extraComponents, Bot bot) {
        for (MessageEmbed embed : embeds) {
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

            extraComponents.add(
                    Component
                            .text("[Embed]")
                            .hoverEvent(HoverEvent.showText(hoverEvent))
                            .color(NamedTextColor.GREEN)
            );
        }
    }

    private void addStickersComponent (List<StickerItem> stickers, List<Component> extraComponents) {
        for (StickerItem sticker : stickers) {
            extraComponents.add(
                    Component
                            .translatable(
                                    "[%s]",
                                    Component
                                            .text(sticker.getName())
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            Component
                                                                    .text(sticker.getId())
                                                                    .color(NamedTextColor.GREEN)
                                                    )
                                            )
                                            .clickEvent(
                                                    ClickEvent.openUrl(
                                                            sticker.getIconUrl()
                                                    )
                                            )
                            )
                            .color(NamedTextColor.GREEN)
            );
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

    public void sendMessageInstantly (String message, String channelId) { sendMessageInstantly(message, channelId, true); }
    public MessageCreateAction sendMessageInstantly (String message, String channelId, boolean queue) {
        if (jda == null) return null;

        final TextChannel logChannel = jda.getTextChannelById(channelId);

        if (logChannel == null) {
            LoggerUtilities.error("Log channel for " + channelId + " is null");
            return null;
        }

        if (queue) {
            logChannel.sendMessage(message).queue(
                    (msg) -> doneSendingInLogs.put(channelId, true),
                    (e) -> {
                        LoggerUtilities.error(e);

                        doneSendingInLogs.put(channelId, false);
                    }
            );

            return null;
        } else {
            return logChannel.sendMessage(message);
        }
    }

    public void onDiscordTick(String channelId) {
        synchronized (logMessages) {
            if (!logMessages.containsKey(channelId) || logMessages.get(channelId).isEmpty()) {
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
                message = logMessage.toString().replace(".", "\u200b.\u200b"); // the ZWSP fixes discord.gg showing invite
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
