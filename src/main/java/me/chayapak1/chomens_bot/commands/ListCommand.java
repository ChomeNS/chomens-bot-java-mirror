package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
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
                new String[] {},
                new String[] { "players" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        final List<PlayerEntry> list = bot.players.list;

        final List<Component> playersComponent = new ArrayList<>();

        for (final PlayerEntry entry : list) {
            if (entry == null) continue;

            // chayapak
            // b58cac19-066b-307b-97b1-d6e19ed08d7c
            //
            // Usernames: foo, bar, baz or No other usernames associated
            // Vanished: false
            // Latency: 32
            // Game Mode: CREATIVE
            // IP Address: 127.0.0.1
            //
            // Click to copy the username to your clipboard
            // Shift+Click to insert the UUID into your chat box
            final Component hoverEvent = Component
                    .text(entry.profile.getName())
                    .append(Component.newline())
                    .append(Component.text(entry.profile.getIdAsString(), bot.colorPalette.uuid))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(
                            entry.persistingData.usernames.isEmpty() ?
                                    Component.translatable(
                                            "commands.list.no_other_usernames",
                                            NamedTextColor.GRAY
                                    ) :
                                    Component.translatable(
                                            "commands.list.with_usernames",
                                            bot.colorPalette.secondary,
                                            Component
                                                    .join(
                                                            JoinConfiguration.commas(true),
                                                            entry.persistingData.usernames
                                                                    .stream()
                                                                    .map(Component::text)
                                                                    .toList()
                                                    )
                                                    .color(bot.colorPalette.string)
                                    )
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "commands.list.vanished",
                                    bot.colorPalette.secondary,
                                    Component.text(!entry.persistingData.listed, bot.colorPalette.string)
                            )
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "commands.list.latency",
                                    bot.colorPalette.secondary,
                                    Component
                                            .text(entry.latency, bot.colorPalette.string) // using number color palette will not blend in (GOLD)
                                            .append(Component.text("ms"))
                            )
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "commands.list.game_mode",
                                    bot.colorPalette.secondary,
                                    Component.text(entry.gamemode.name(), bot.colorPalette.string)
                            )
                    )
                    .append(Component.newline())
                    .append(
                            Component.translatable(
                                    "commands.list.ip_address",
                                    bot.colorPalette.secondary,
                                    Component.text(
                                            entry.persistingData.ip == null
                                                    ? "N/A"
                                                    : entry.persistingData.ip,
                                            bot.colorPalette.string
                                    )
                            )
                    )
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.translatable("commands.generic.click_to_copy_username", NamedTextColor.GREEN))
                    .append(Component.newline())
                    .append(Component.translatable("commands.generic.shift_click_to_insert_uuid", NamedTextColor.GREEN));

            Component component;

            if (context.inGame) {
                component = Component.translatable(
                        "%s",
                        entry.displayName == null ?
                                Component.text(entry.profile.getName()) :
                                entry.displayName
                );
            } else {
                component = Component.translatable(
                        "%s (%s - %s - %s)",
                        NamedTextColor.DARK_GRAY,
                        entry.displayName == null ?
                                Component.text(entry.profile.getName(), NamedTextColor.WHITE) :
                                entry.displayName.colorIfAbsent(NamedTextColor.WHITE),
                        Component.text(entry.profile.getName(), NamedTextColor.WHITE),
                        Component.text(entry.profile.getIdAsString(), bot.colorPalette.uuid),
                        Component.text(
                                entry.persistingData.ip == null
                                        ? "N/A"
                                        : entry.persistingData.ip,
                                bot.colorPalette.string
                        )
                );
            }

            component = component
                    .hoverEvent(HoverEvent.showText(hoverEvent))
                    .clickEvent(ClickEvent.copyToClipboard(entry.profile.getName()))
                    .insertion(entry.profile.getIdAsString());

            playersComponent.add(component);
        }

        return Component.empty()
                .append(Component.translatable("commands.list.players_text", NamedTextColor.GREEN))
                .append(Component.text("(", NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size(), NamedTextColor.GRAY))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .append(Component.newline())
                .append(Component.join(JoinConfiguration.newlines(), playersComponent));
    }
}
