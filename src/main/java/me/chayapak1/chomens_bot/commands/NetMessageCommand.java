package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class NetMessageCommand implements Command {
    public String name() { return "netmsg"; }

    public String description() {
        return "Broadcasts a message to all of the servers that the bot is connected";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{message}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("networkmessage");
        aliases.add("irc");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();
        final List<Bot> bots = bot.allBots();

        final String hostAndPort = bot.host() + ":" + bot.port();

        final Component component = Component.translatable(
                "[%s] %s › %s",
                Component
                        .text(hostAndPort)
                        .color(NamedTextColor.GRAY)
                        .clickEvent(ClickEvent.copyToClipboard(hostAndPort))
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component.empty()
                                                .append(Component.text(hostAndPort).color(NamedTextColor.GRAY))
                                                .append(Component.newline())
                                                .append(Component.text("Click here to copy the server host and port to your clipboard").color(NamedTextColor.GREEN))
                                )
                        ),
                context.displayName().color(NamedTextColor.GRAY),
                Component.text(String.join(" ", args)).color(NamedTextColor.GRAY)
        ).color(NamedTextColor.DARK_GRAY);

        for (Bot allBot : bots) {
            allBot.chat().tellraw(component);
        }

        return Component.text("success");
    }
}
