package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ListCommand extends Command {
    public ListCommand () {
        super(
                "list",
                "Lists all players in the server (including vanished)",
                new String[] {},
                new String[] { "players" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        final List<PlayerEntry> list = bot.players.list;

        final List<Component> playersComponent = new ArrayList<>();

        for (PlayerEntry entry : list) {
            if (entry == null) continue;
            playersComponent.add(
                    Component.translatable(
                            "%s › %s",
                            entry.displayName == null ?
                                    Component.text(entry.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)) :
                                    entry.displayName
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component
                                                            .text(entry.profile.getName())
                                                            .append(Component.newline())
                                                            .append(Component.text("Click here to copy the username to your clipboard").color(NamedTextColor.GREEN))
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile.getName())
                                    )
                                    .color(NamedTextColor.WHITE),
                            Component
                                    .text(entry.profile.getIdAsString())
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component.text("Click here to copy the UUID to your clipboard").color(NamedTextColor.GREEN)
                                            )
                                    )
                                    .clickEvent(
                                            ClickEvent.copyToClipboard(entry.profile.getIdAsString())
                                    )
                                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.uuid))
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }

        return Component.empty()
                .append(Component.text("Players ").color(NamedTextColor.GREEN))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size()).color(NamedTextColor.GRAY))
                .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(
                        Component.join(JoinConfiguration.newlines(), playersComponent)
                );
    }
}
