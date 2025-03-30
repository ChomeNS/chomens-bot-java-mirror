package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
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

            // chayapak
            // b58cac19-066b-307b-97b1-d6e19ed08d7c
            //
            // Usernames: foo, bar, baz or No other usernames associated
            // Vanished: false
            // Latency: 32
            // Game Mode: CREATIVE
            //
            // Click to copy the username to your clipboard
            // Shift+Click to insert the UUID into your chat box
            final Component hoverEvent = Component
                    .text(entry.profile.getName())
                    .append(Component.newline())
                    .append(Component.text(entry.profile.getIdAsString()).color(ColorUtilities.getColorByString(bot.config.colorPalette.uuid)))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(
                            entry.usernames.isEmpty() ?
                                    Component
                                            .text("No other usernames associated")
                                            .color(NamedTextColor.GRAY) :
                                    Component.translatable(
                                            "Usernames: %s",
                                            Component
                                                    .join(
                                                            JoinConfiguration.commas(true),
                                                            entry.usernames
                                                                    .stream()
                                                                    .map(Component::text)
                                                                    .toList()
                                                    )
                                                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "Vanished: %s",
                                    Component
                                            .text(!entry.listed)
                                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                            ).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "Latency: %s",
                                    Component
                                            .text(entry.latency)
                                            .append(Component.text("ms"))
                                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.string)) // using number color palette will not blend in (GOLD)
                            ).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "Game Mode: %s",
                                    Component
                                            .text(entry.gamemode.name())
                                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                            ).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Click to copy the username to your clipboard").color(NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.text("Shift+Click to insert the UUID into your chat box").color(NamedTextColor.GREEN));

            playersComponent.add(
                    Component.translatable(
                            "%s",
                            entry.displayName == null ?
                                    Component.text(entry.profile.getName()) :
                                    entry.displayName,
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
                    )
                    .hoverEvent(
                            HoverEvent.showText(hoverEvent)
                    )
                    .clickEvent(
                            ClickEvent.copyToClipboard(entry.profile.getName())
                    )
                    .insertion(entry.profile.getIdAsString())
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
