package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.Mail;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.MailPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MailCommand extends Command {
    public MailCommand () {
        super(
                "mail",
                "Sends a mail",
                new String[] { "send <player> <message>", "sendselecteditem <player>", "read" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final PlayerEntry sender = context.sender;

        final ObjectMapper objectMapper = MailPlugin.objectMapper;

        // kinda messy ngl

        final String action = context.getString(false, true, true);

        switch (action) {
            case "send" -> {
                bot.mail.send(
                        new Mail(
                                sender.profile.getName(),
                                context.getString(false, true),
                                Instant.now().toEpochMilli(),
                                bot.host + ":" + bot.port,
                                context.getString(true, true)
                        )
                );

                return Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "sendselecteditem" -> {
                context.checkOverloadArgs(2);

                final CompletableFuture<Component> future = bot.core.runTracked(
                        "minecraft:data get entity " +
                                UUIDUtilities.selector(sender.profile.getId()) +
                                " SelectedItem.components.minecraft:custom_data.message"
                );

                if (future == null) {
                    throw new CommandException(Component.text("There was an error while sending your mail"));
                }

                future.thenApplyAsync(output -> {
                    try {
                        final List<Component> children = output.children();

                        if (
                                !children.isEmpty() &&
                                        children.get(0).children().isEmpty() &&
                                        ((TranslatableComponent) children.get(0)).key()
                                                .equals("arguments.nbtpath.nothing_found")
                        ) {
                            context.sendOutput(Component.text("Player has no `message` NBT tag in their selected item's minecraft:custom_data").color(NamedTextColor.RED));
                            return output;
                        }

                        final Component actualOutputComponent = ((TranslatableComponent) output).arguments().get(1).asComponent();

                        final String value = ComponentUtilities.stringify(actualOutputComponent);

                        if (!value.startsWith("\"") && !value.endsWith("\"") && !value.startsWith("'") && !value.endsWith("'")) {
                            context.sendOutput(Component.text("`message` NBT is not a string").color(NamedTextColor.RED));
                            return output;
                        }

                        bot.mail.send(
                                new Mail(
                                        sender.profile.getName(),
                                        context.getString(true, true),
                                        Instant.now().toEpochMilli(),
                                        bot.host + ":" + bot.port,
                                        value.substring(1).substring(0, value.length() - 2)
                                )
                        );
                    } catch (CommandException e) {
                        context.sendOutput(e.message.color(NamedTextColor.RED));
                        return output;
                    }

                    context.sendOutput(
                            Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                    );

                    return output;
                });
            }
            case "read" -> {
                context.checkOverloadArgs(1);

                // TODO: use less for loops?

                int senderMailSize = 0;
                for (JsonNode mailElement : MailPlugin.mails.deepCopy()) {
                    try {
                        final Mail mail = objectMapper.treeToValue(mailElement, Mail.class);

                        if (!mail.sentTo.equals(sender.profile.getName())) continue;
                        senderMailSize++;
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                if (senderMailSize == 0) {
                    throw new CommandException(Component.text("You have no new mails"));
                }

                final List<Component> mailsComponent = new ArrayList<>();

                int count = 1;
                for (JsonNode mailElement : MailPlugin.mails.deepCopy()) {
                    try {
                        final Mail mail = objectMapper.treeToValue(mailElement, Mail.class);

                        if (!mail.sentTo.equals(sender.profile.getName())) continue;

                        final Instant instant = Instant.ofEpochMilli(mail.timeSent);
                        final ZoneId zoneId = ZoneId.systemDefault();
                        final OffsetDateTime localDateTime = OffsetDateTime.ofInstant(instant, zoneId);

                        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm:ss a Z");
                        final String formattedTime = localDateTime.format(formatter);

                        mailsComponent.add(
                                Component.translatable(
                                        """
                                                %s %s Sent by: %s %s
                                                Contents:
                                                %s""",
                                        Component.text(count).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                        Component.text("-").color(NamedTextColor.DARK_GRAY),

                                        Component.text(mail.sentBy).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                                        Component
                                                .text("[Hover here for more info]")
                                                .color(NamedTextColor.GREEN)
                                                .hoverEvent(
                                                        HoverEvent.showText(
                                                                Component.translatable(
                                                                        """
                                                                                Time sent: %s
                                                                                Server: %s""",
                                                                        Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                                                                        Component.text(mail.server).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                                                                ).color(NamedTextColor.GREEN)
                                                        )
                                                ),
                                        Component.text(mail.contents).color(NamedTextColor.WHITE)
                                ).color(NamedTextColor.GREEN)
                        );

                        count++;
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                final Component component = Component.empty()
                        .append(Component.text("Mails ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(senderMailSize).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(Component.join(JoinConfiguration.newlines(), mailsComponent));

                if (context.inGame) {
                    bot.chat.tellraw(
                            component,
                            context.sender.profile.getId()
                    );
                } else {
                    context.sendOutput(component);
                }

                for (JsonNode mailElement : MailPlugin.mails.deepCopy()) {
                    try {
                        final Mail mail = objectMapper.treeToValue(mailElement, Mail.class);

                        if (mail.sentTo.equals(sender.profile.getName())) bot.mail.remove(mailElement);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                PersistentDataUtilities.put("mails", MailPlugin.mails);
            }
            default -> context.sendOutput(Component.text("Invalid action").color(NamedTextColor.RED));
        }

        return null;
    }
}
