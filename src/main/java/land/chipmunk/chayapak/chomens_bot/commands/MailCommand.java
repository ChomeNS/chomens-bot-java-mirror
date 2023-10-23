package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.Mail;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.plugins.MailPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.PersistentDataUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.time.Instant;
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

        final Gson gson = new Gson();

        // kinda messy ngl

        final String action = context.getString(false, true, true);

        switch (action) {
            case "send" -> {
                int senderMailsSentTotal = 0;
                for (JsonElement mailElement : MailPlugin.mails) {
                    final Mail mail = gson.fromJson(mailElement, Mail.class);

                    if (mail.sentBy == null) continue;

                    if (!mail.sentBy.equals(sender.profile.getName())) continue;
                    senderMailsSentTotal++;
                }

                if (senderMailsSentTotal > 256) {
                    throw new CommandException(Component.text("You are sending too many mails!"));
                }

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
                context.checkOverloadArgs(1);

                int senderMailsSentTotal = 0;
                for (JsonElement mailElement : MailPlugin.mails) {
                    final Mail mail = gson.fromJson(mailElement, Mail.class);

                    if (!mail.sentTo.equals(sender.profile.getName())) continue;
                    senderMailsSentTotal++;
                }

                if (senderMailsSentTotal > 256) {
                    throw new CommandException(Component.text("You are sending too many mails!"));
                }

                final CompletableFuture<Component> future = bot.core.runTracked(
                        "minecraft:data get entity " +
                                UUIDUtilities.selector(sender.profile.getId()) +
                                " SelectedItem.tag.message"
                );

                if (future == null) {
                    throw new CommandException(Component.text("There was an error while sending your mail"));
                }

                future.thenApply(output -> {
                    final List<Component> children = output.children();

                    if (
                            !children.isEmpty() &&
                                    !children.get(0).children().isEmpty() &&
                                    ((TranslatableComponent) children.get(0).children().get(0))
                                            .key()
                                            .equals("arguments.nbtpath.nothing_found")
                    ) {
                        context.sendOutput(Component.text("Player has no `message` NBT tag in the selected item").color(NamedTextColor.RED));
                        return output;
                    }

                    final String value = ComponentUtilities.stringify(((TranslatableComponent) children.get(0)).args().get(1));

                    if (!value.startsWith("\"") && !value.endsWith("\"") && !value.startsWith("'") && !value.endsWith("'")) {
                        context.sendOutput(Component.text("`message` NBT is not a string").color(NamedTextColor.RED));
                        return output;
                    }

                    try {
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
                for (JsonElement mailElement : MailPlugin.mails) {
                    final Mail mail = gson.fromJson(mailElement, Mail.class);

                    if (!mail.sentTo.equals(sender.profile.getName())) continue;
                    senderMailSize++;
                }

                if (senderMailSize == 0) {
                    throw new CommandException(Component.text("You have no new mails"));
                }

                final List<Component> mailsComponent = new ArrayList<>();

                int i = 1;
                for (JsonElement mailElement : MailPlugin.mails) {
                    final Mail mail = gson.fromJson(mailElement, Mail.class);

                    if (!mail.sentTo.equals(sender.profile.getName())) continue;

                    final DateTimeFormatter formatter = DateTimeFormat.forPattern("MMMM d, YYYY, hh:mm:ss a Z");
                    final String formattedTime = formatter.print(mail.timeSent);

                    mailsComponent.add(
                            Component.translatable(
                                    """
                                            %s %s Sent by: %s %s
                                            Contents:
                                            %s""",
                                    Component.text(i).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
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

                    i++;
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

                for (JsonElement mailElement : MailPlugin.mails.deepCopy()) {
                    final Mail mail = gson.fromJson(mailElement, Mail.class);

                    if (mail.sentTo.equals(sender.profile.getName())) MailPlugin.mails.remove(mailElement);
                }

                PersistentDataUtilities.put("mails", MailPlugin.mails);
            }
            default -> context.sendOutput(Component.text("Invalid action").color(NamedTextColor.RED));
        }

        return null;
    }
}
