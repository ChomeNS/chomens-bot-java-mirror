package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.data.Mail;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.plugins.MailPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MailCommand implements Command {
    public String name() { return "mail"; }

    public String description() {
        return "Sends a mail";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("send <player> <{message}>");
        usages.add("sendselecteditem <player>");
        usages.add("read");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final MutablePlayerListEntry sender = context.sender();

        // kinda messy ngl

        switch (args[0]) {
            case "send" -> {
                int senderMailsSentTotal = 0;
                for (Mail mail : MailPlugin.mails()) {
                    if (!mail.sentBy().equals(sender.profile().getName())) continue;
                    senderMailsSentTotal++;
                }

                if (senderMailsSentTotal > 256) return Component.text("You are sending too much mails!").color(NamedTextColor.RED);

                bot.mail().send(
                        new Mail(
                                sender.profile().getName(),
                                args[1],
                                DateTime.now(),
                                bot.host() + ":" + bot.port(),
                                String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                        )
                );

                return Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
            }
            case "sendselecteditem" -> {
                int senderMailsSentTotal = 0;
                for (Mail mail : MailPlugin.mails()) {
                    if (!mail.sentTo().equals(sender.profile().getName())) continue;
                    senderMailsSentTotal++;
                }

                if (senderMailsSentTotal > 256) return Component.text("You are sending too much mails!").color(NamedTextColor.RED);

                final CompletableFuture<CompoundTag> future = bot.core().runTracked(
                        "minecraft:data get entity " +
                                UUIDUtilities.selector(sender.profile().getId()) +
                                " SelectedItem.tag.message"
                );

                if (future == null) return Component.text("There was an error while sending your mail").color(NamedTextColor.RED);

                future.thenApply(tags -> {
                    if (!tags.contains("LastOutput") || !(tags.get("LastOutput") instanceof StringTag)) return tags;

                    final StringTag lastOutput = tags.get("LastOutput");

                    final Component output = GsonComponentSerializer.gson().deserialize(lastOutput.getValue());

                    final List<Component> children = output.children();

                    // too lazy to use translation,.,.,.
                    final String parsed = ComponentUtilities.stringify(children.get(0));

                    if (parsed.startsWith("Found no elements matching ")) {
                        context.sendOutput(Component.text("Player has no `message` NBT tag in the selected item").color(NamedTextColor.RED));
                        return tags;
                    }

                    final String value = ComponentUtilities.stringify(((TranslatableComponent) children.get(0)).args().get(1));

                    if (!value.startsWith("\"") && !value.endsWith("\"") && !value.startsWith("'") && !value.endsWith("'")) {
                        context.sendOutput(Component.text("`message` NBT is not a string").color(NamedTextColor.RED));
                        return tags;
                    }

                    bot.mail().send(
                            new Mail(
                                    sender.profile().getName(),
                                    args[1],
                                    DateTime.now(),
                                    bot.host() + ":" + bot.port(),
                                    value.substring(1).substring(0, value.length() - 2)
                            )
                    );

                    context.sendOutput(
                            Component.text("Mail sent!").color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
                    );

                    return tags;
                });

                return null;
            }
            case "read" -> {
                // TODO: use less for loops?

                int senderMailSize = 0;
                for (Mail mail : MailPlugin.mails()) {
                    if (!mail.sentTo().equals(sender.profile().getName())) continue;
                    senderMailSize++;
                }

                if (senderMailSize == 0) return Component.text("You have no new mails").color(NamedTextColor.RED);

                final List<Component> mailsComponent = new ArrayList<>();

                int i = 1;
                for (Mail mail : MailPlugin.mails()) {
                    if (!mail.sentTo().equals(sender.profile().getName())) continue;

                    final DateTimeFormatter formatter = DateTimeFormat.forPattern("MMMM d, YYYY, hh:mm:ss a Z");
                    final String formattedTime = formatter.print(mail.timeSent());

                    mailsComponent.add(
                            Component.translatable(
                                    """
                                            %s %s Sent by: %s %s
                                            Contents:
                                            %s""",
                                    Component.text(i).color(ColorUtilities.getColorByString(bot.config().colorPalette().number())),
                                    Component.text("-").color(NamedTextColor.DARK_GRAY),

                                    Component.text(mail.sentBy()).color(ColorUtilities.getColorByString(bot.config().colorPalette().username())),
                                    Component
                                            .text("[Hover here for more info]")
                                            .color(NamedTextColor.GREEN)
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            Component.translatable(
                                                                    """
                                                                            Time sent: %s
                                                                            Server: %s""",
                                                                    Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config().colorPalette().string())),
                                                                    Component.text(mail.server()).color(ColorUtilities.getColorByString(bot.config().colorPalette().string()))
                                                            ).color(NamedTextColor.GREEN)
                                                    )
                                            ),
                                    Component.text(mail.contents()).color(NamedTextColor.WHITE)
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

                if (context.inGame()) {
                    bot.chat().tellraw(
                            component,
                            context.sender().profile().getId()
                    );
                } else {
                    context.sendOutput(component);
                }

                // thanks https://www.baeldung.com/java-concurrentmodificationexception#3-using-removeif:~:text=3.3.%20Using%20removeIf()
                MailPlugin.mails().removeIf(mail -> mail.sentTo().equals(sender.profile().getName()));

                return null;
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }
    }
}
