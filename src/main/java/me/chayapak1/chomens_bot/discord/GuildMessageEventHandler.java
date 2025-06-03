package me.chayapak1.chomens_bot.discord;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.contexts.DiscordCommandContext;
import me.chayapak1.chomens_bot.util.ChatMessageUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.messages.MessageSnapshot;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuildMessageEventHandler extends ListenerAdapter {
    private final String prefix;
    private final Component messagePrefix;

    private final JDA jda;

    public GuildMessageEventHandler (
            final JDA jda,
            final String prefix,
            final Component messagePrefix
    ) {
        this.jda = jda;
        this.prefix = prefix;
        this.messagePrefix = messagePrefix;

        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived (@NotNull final MessageReceivedEvent event) {
        if (event.getAuthor().getId().equals(jda.getSelfUser().getId())) return;

        for (final Bot bot : Main.bots) {
            final String channelId = bot.options.discordChannelId;

            if (channelId == null || !event.getChannel().getId().equals(channelId)) continue;

            final Message messageObject = event.getMessage();
            final String messageString = messageObject.getContentDisplay();

            if (messageString.startsWith(prefix)) {
                final DiscordCommandContext context = new DiscordCommandContext(
                        bot,
                        prefix,
                        event.getMember(),
                        event.getAuthor().getName(),
                        fileUpload -> messageObject.replyFiles(fileUpload).queue(),
                        embed -> messageObject.replyEmbeds(embed).queue()
                );

                bot.commandHandler.executeCommand(messageString.substring(prefix.length()), context);

                return;
            }

            final boolean isForwarded = !messageObject.getMessageSnapshots().isEmpty();

            Component output = Component.empty();

            if (isForwarded) {
                for (final MessageSnapshot snapshot : messageObject.getMessageSnapshots()) {
                    final Component messageComponent = getForwardedMessageComponent(bot, snapshot);

                    output = Component.translatable(
                            "[%s] %s › %s",
                            NamedTextColor.DARK_GRAY,
                            this.messagePrefix,
                            Component
                                    .translatable(
                                            "%s forwarded",
                                            NamedTextColor.GRAY,
                                            Component.text(
                                                    messageObject.getMember() == null ?
                                                            messageObject.getAuthor().getName() :
                                                            messageObject.getMember().getEffectiveName(),
                                                    NamedTextColor.RED
                                            )
                                    ),
                            messageComponent.color(NamedTextColor.GRAY)
                    );
                }
            } else {
                final Message reference = event.getMessage().getReferencedMessage();

                if (reference != null) {
                    output = output
                            .append(
                                    Component.empty()
                                            .append(Component.text("Replying to ", NamedTextColor.GRAY))
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

    private Component getForwardedMessageComponent (final Bot bot, final MessageSnapshot snapshot) {
        final List<Component> extraComponents = new ArrayList<>();

        addExtraComponents(
                extraComponents,
                snapshot.getAttachments(),
                snapshot.getEmbeds(),
                snapshot.getStickers(),
                bot
        );

        final String replacedMessageContent = replaceMessageContent(snapshot.getContentRaw());

        Component messageComponent = Component.text(replacedMessageContent);

        if (!extraComponents.isEmpty()) {
            if (!replacedMessageContent.isBlank())
                messageComponent = messageComponent.append(Component.space());

            messageComponent = messageComponent
                    .append(
                            Component.join(
                                    JoinConfiguration.spaces(),
                                    extraComponents
                            )
                    );
        }

        return messageComponent;
    }

    private Component getMessageComponent (
            final Bot bot,
            final Message message
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
                    .append(Component.text("Roles:", NamedTextColor.GRAY))
                    .append(Component.newline());

            final List<Component> rolesList = new ArrayList<>();

            for (final Role role : roles) {
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
            rolesComponent = rolesComponent.append(Component.text("No roles", NamedTextColor.GRAY));
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
                                        NamedTextColor.DARK_GRAY,
                                        Component.text(username, NamedTextColor.WHITE),
                                        rolesComponent,
                                        Component.text("Click here to copy the tag to your clipboard", NamedTextColor.GREEN)
                                )
                        )
                );

        for (final Role role : roles) {
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

        final String replacedMessageContent = replaceMessageContent(message.getContentDisplay());

        Component actualMessage = ChatMessageUtilities.applyChatMessageStyling(replacedMessageContent);

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
                NamedTextColor.DARK_GRAY,
                messagePrefix,
                nameComponent,
                messageComponent
        );
    }

    private String replaceMessageContent (final String content) {
        return ComponentUtilities
                // replaces the ANSI codes (from the bot) with section signs corresponding to the color/style
                .deserializeFromDiscordAnsi(content)
                // replaces skull emoji
                .replace("\uD83D\uDC80", "☠")
                // replaces all ZWSP with nothing
                .replace("\u200b", "");
    }

    private void addExtraComponents (
            final List<Component> extraComponents,
            final List<Message.Attachment> attachments,
            final List<MessageEmbed> embeds,
            final List<StickerItem> stickers,
            final Bot bot
    ) {
        addAttachmentsComponent(attachments, extraComponents);
        addEmbedsComponent(embeds, extraComponents, bot);
        addStickersComponent(stickers, extraComponents);
    }

    private void addAttachmentsComponent (final List<Message.Attachment> attachments, final List<Component> extraComponents) {
        for (final Message.Attachment attachment : attachments) {
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

    private void addEmbedsComponent (final List<MessageEmbed> embeds, final List<Component> extraComponents, final Bot bot) {
        for (final MessageEmbed embed : embeds) {
            final Component hoverEvent = Component.translatable(
                    """
                            Title: %s
                            %s""",
                    NamedTextColor.GREEN,
                    embed.getTitle() == null ?
                            Component.text("No title", NamedTextColor.GRAY) :
                            Component.text(embed.getTitle(), bot.colorPalette.string),
                    embed.getDescription() == null ?
                            Component.text("No description", NamedTextColor.GRAY) :
                            Component.text(ComponentUtilities.deserializeFromDiscordAnsi(embed.getDescription()), NamedTextColor.WHITE)
            );

            extraComponents.add(
                    Component
                            .text("[Embed]", NamedTextColor.GREEN)
                            .hoverEvent(HoverEvent.showText(hoverEvent))
            );
        }
    }

    private void addStickersComponent (final List<StickerItem> stickers, final List<Component> extraComponents) {
        for (final StickerItem sticker : stickers) {
            extraComponents.add(
                    Component
                            .translatable(
                                    "[%s]",
                                    NamedTextColor.GREEN,
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
            );
        }
    }
}
