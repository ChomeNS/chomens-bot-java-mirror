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
                new String[] { "send <player> <message>", "sendselecteditem <player>", "read" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (Main.database == null)
            throw new CommandException(Component.translatable("commands.generic.error.database_disabled"));

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

                    context.sendOutput(Component.translatable("commands.mail.sent", bot.colorPalette.defaultColor));
                } catch (final CommandException e) {
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
                            throw new CommandException(Component.translatable("commands.mail.sendselecteditem.error.no_item_nbt"));
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

                                context.sendOutput(Component.translatable("commands.mail.sent", bot.colorPalette.defaultColor));
                            } catch (final CommandException e) {
                                context.sendOutput(e.message.color(NamedTextColor.RED));
                            }
                        });
                    } catch (final CommandException e) {
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
                    for (final Mail mail : mails) {
                        if (!mail.sentTo().equals(sender.profile.getName())) continue;
                        senderMailSize++;
                    }

                    if (senderMailSize == 0) {
                        context.sendOutput(Component.translatable("commands.mail.read.no_new_mails", NamedTextColor.RED));
                        return;
                    }

                    final int tempFinalSenderMailSize = senderMailSize;
                    final List<Component> mailsComponent = new ArrayList<>();

                    int count = 1;
                    for (final Mail mail : mails) {
                        if (!mail.sentTo().equals(sender.profile.getName())) continue;

                        final Instant instant = Instant.ofEpochMilli(mail.timeSent());
                        final ZoneId zoneId = ZoneId.systemDefault();
                        final OffsetDateTime localDateTime = OffsetDateTime.ofInstant(instant, zoneId);

                        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy, hh:mm:ss a Z");
                        final String formattedTime = localDateTime.format(formatter);

                        mailsComponent.add(
                                Component.translatable(
                                        "commands.mail.read.mail_contents",
                                        Component.text(count, bot.colorPalette.number),
                                        Component.text("-", NamedTextColor.DARK_GRAY),

                                        Component.text(mail.sentBy(), bot.colorPalette.username),
                                        Component
                                                .translatable("commands.mail.read.hover_more_info", NamedTextColor.GREEN)
                                                .hoverEvent(
                                                        HoverEvent.showText(
                                                                Component.translatable(
                                                                        "commands.mail.read.hover_info",
                                                                        NamedTextColor.GREEN,
                                                                        Component.text(formattedTime, bot.colorPalette.string),
                                                                        Component.text(mail.server(), bot.colorPalette.string)
                                                                )
                                                        )
                                                ),
                                        Component.text(mail.contents(), NamedTextColor.WHITE)
                                ).color(NamedTextColor.GREEN)
                        );

                        count++;
                    }

                    final Component component = Component.empty()
                            .append(Component.translatable("commands.mail.read.mails_text").color(NamedTextColor.GREEN))
                            .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                            .append(Component.text(tempFinalSenderMailSize).color(NamedTextColor.GRAY))
                            .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                            .append(Component.newline())
                            .append(Component.join(JoinConfiguration.newlines(), mailsComponent));

                    final Component renderedComponent = bot.commandHandler.renderTranslatable(component);

                    if (context.inGame) {
                        bot.chat.tellraw(
                                renderedComponent,
                                context.sender.profile.getId()
                        );
                    } else {
                        context.sendOutput(renderedComponent);
                    }

                    bot.mail.clear(sender.profile.getName());
                });
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }

        return null;
    }
}
