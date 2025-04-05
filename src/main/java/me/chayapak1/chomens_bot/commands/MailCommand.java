package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.mail.Mail;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (Main.database == null)
            throw new CommandException(Component.text("Database is not enabled in the bot's config"));

        Main.database.checkOverloaded();

        final PlayerEntry sender = context.sender;

        // kinda messy ngl

        final String action = context.getAction();

        switch (action) {
            case "send" -> DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
                try {
                    bot.mail.send(
                            new Mail(
                                    sender.profile.getName(),
                                    context.getString(false, true),
                                    Instant.now().toEpochMilli(),
                                    bot.getServerString(),
                                    context.getString(true, true)
                            )
                    );

                    context.sendOutput(Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
                } catch (CommandException e) {
                    context.sendOutput(e.message.color(NamedTextColor.RED));
                }
            });
            case "sendselecteditem" -> {
                context.checkOverloadArgs(2);

                final CompletableFuture<String> future = bot.query.entity(
                        context.sender.profile.getIdAsString(),
                        "SelectedItem.components.minecraft:custom_data.message"
                );

                future.thenApply(output -> {
                    try {
                        if (output.isEmpty()) {
                            throw new CommandException(Component.text("Player has no `message` NBT tag in their selected item's minecraft:custom_data"));
                        }

                        DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
                            try {
                                bot.mail.send(
                                        new Mail(
                                                sender.profile.getName(),
                                                context.getString(true, true),
                                                Instant.now().toEpochMilli(),
                                                bot.getServerString(),
                                                output
                                        )
                                );

                                context.sendOutput(
                                        Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                                );
                            } catch (CommandException e) {
                                context.sendOutput(e.message.color(NamedTextColor.RED));
                            }
                        });
                    } catch (CommandException e) {
                        context.sendOutput(e.message.color(NamedTextColor.RED));
                        return null;
                    }

                    return output;
                });
            }
            case "read" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
                    final List<Mail> mails = bot.mail.list();

                    int senderMailSize = 0;
                    for (Mail mail : mails) {
                        if (!mail.sentTo().equals(sender.profile.getName())) continue;
                        senderMailSize++;
                    }

                    if (senderMailSize == 0) {
                        context.sendOutput(Component.text("You have no new mails").color(NamedTextColor.RED));
                        return;
                    }

                    final int tempFinalSenderMailSize = senderMailSize;
                    final List<Component> mailsComponent = new ArrayList<>();

                    int count = 1;
                    for (Mail mail : mails) {
                        if (!mail.sentTo().equals(sender.profile.getName())) continue;

                        final Instant instant = Instant.ofEpochMilli(mail.timeSent());
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

                                        Component.text(mail.sentBy()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
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
                                                                        Component.text(mail.server()).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                                                                ).color(NamedTextColor.GREEN)
                                                        )
                                                ),
                                        Component.text(mail.contents()).color(NamedTextColor.WHITE)
                                ).color(NamedTextColor.GREEN)
                        );

                        count++;
                    }

                    final Component component = Component.empty()
                            .append(Component.text("Mails ").color(NamedTextColor.GREEN))
                            .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text(tempFinalSenderMailSize).color(NamedTextColor.GRAY))
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

                    bot.mail.clear(sender.profile.getName());
                });
            }
            default -> context.sendOutput(Component.text("Invalid action").color(NamedTextColor.RED));
        }

        return null;
    }
}
