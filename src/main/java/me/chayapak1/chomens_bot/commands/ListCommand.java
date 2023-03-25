package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements Command {
    public String name() { return "list"; }

    public String description() {
        return "Lists all players in the server (including vanished)";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

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

        final List<MutablePlayerListEntry> list = bot.players().list();

        final List<Component> playersComponent = new ArrayList<>();

        for (MutablePlayerListEntry entry : list) {
            playersComponent.add(
                    Component.translatable(
                            "%s › %s",
                            Component
                                    .text(entry.profile().getName())
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component.text("Click here to copy the username to your clipboard").color(NamedTextColor.GREEN)
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile().getName())
                                    )
                                    .color(NamedTextColor.YELLOW),
                            Component
                                    .text(entry.profile().getIdAsString())
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile().getIdAsString())
                                    )
                                    .color(NamedTextColor.AQUA)
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }

        final Component component = Component.empty()
                .append(Component.text("Players ").color(NamedTextColor.GREEN))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size()).color(NamedTextColor.GRAY))
                .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(
                        Component.join(JoinConfiguration.newlines(), playersComponent)
                );

        context.sendOutput(component);

        return Component.text("success");
    }
}
