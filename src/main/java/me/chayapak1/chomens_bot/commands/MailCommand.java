package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.mail.Mail;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.util.TimeUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MailCommand extends Command {
    public MailCommand () {
        super(
                "mail",
                new String[] { "send <player> <message>", "sendselecteditem <player>", "read" },
                new String[] {},
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[] { ChatPacketType.DISGUISED }
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
            case "send" -> DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
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
                    context.sendOutput(e.message.colorIfAbsent(NamedTextColor.RED));
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
                        if (output == null) {
                            throw new CommandException(Component.translatable("commands.mail.sendselecteditem.error.no_item_nbt"));
                        }

                        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
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
                                context.sendOutput(e.message.colorIfAbsent(NamedTextColor.RED));
                            }
                        });
                    } catch (final CommandException e) {
                        context.sendOutput(e.message.colorIfAbsent(NamedTextColor.RED));
                        return null;
                    }

                    return output;
                });
            }
            case "read" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
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

                        final String formattedTime = TimeUtilities.formatTime(
                                mail.timeSent(),
                                "MMMM d, yyyy, hh:mm:ss a Z",
                                ZoneId.of("UTC")
                        );

                        mailsComponent.add(
                                Component.translatable(
                                        "commands.mail.read.mail_contents",
                                        NamedTextColor.GREEN,
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
                                )
                        );

                        count++;
                    }

                    final Component component = Component.empty()
                            .append(Component.translatable("commands.mail.read.mails_text", NamedTextColor.GREEN))
                            .append(Component.text("(", NamedTextColor.DARK_GRAY))
                            .append(Component.text(tempFinalSenderMailSize, NamedTextColor.GRAY))
                            .append(Component.text(")", NamedTextColor.DARK_GRAY))
                            .append(Component.newline())
                            .append(Component.join(JoinConfiguration.newlines(), mailsComponent));

                    if (context.inGame) {
                        final Component renderedComponent = I18nUtilities.render(component);

                        bot.chat.tellraw(
                                renderedComponent,
                                context.sender.profile.getId()
                        );
                    } else {
                        context.sendOutput(component);
                    }

                    bot.mail.clear(sender.profile.getName());
                });
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }

        return null;
    }
}
