package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.filter.FilteredPlayer;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.plugins.PlayerFilterPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class FilterCommand extends Command {
    public FilterCommand () {
        super(
                "filter",
                "Filter players",
                new String[] {
                        "add [player]",
                        "-ignorecase add <player> [reason]",
                        "-regex add <player> [reason]",
                        "remove <index>",
                        "clear",
                        "list"
                },
                new String[] { "filterplayer", "ban", "blacklist" },
                TrustLevel.ADMIN,
                false
        );
    }

    // most of these codes are from cloop and greplog
    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        boolean ignoreCase = false;
        boolean regex = false;

        String action = context.getString(false, true, true);

        // run 2 times. for example `*filter -ignorecase -regex add test` will be both accepted
        for (int i = 0; i < 2; i++) {
            if (action.equals("-ignorecase")) {
                ignoreCase = true;
                action = context.getString(false, true);
            } else if (action.equals("-regex")) {
                regex = true;
                action = context.getString(false, true);
            }
        }

        switch (action) {
            case "add" -> {
                final String player = context.getString(false, true);
                final String reason = context.getString(true, false);

                if (
                        PlayerFilterPlugin.localList.stream()
                                .map(filteredPlayer -> filteredPlayer.playerName)
                                .toList()
                                .contains(player)
                ) {
                    throw new CommandException(
                            Component.translatable(
                                    "The player %s is already in the filters",
                                    Component.text(player)
                            )
                    );
                }

                final boolean finalRegex = regex;
                final boolean finalIgnoreCase = ignoreCase;

                DatabasePlugin.executorService.submit(() -> bot.playerFilter.add(player, reason, finalRegex, finalIgnoreCase));

                if (reason.isEmpty()) {
                    return Component.translatable(
                            "Added %s to the filters",
                            Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } else {
                    return Component.translatable(
                            "Added %s to the filters with reason %s",
                            Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                            Component.text(reason).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                }
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                final int index = context.getInteger(true);

                final FilteredPlayer player = PlayerFilterPlugin.localList.get(index);

                if (player == null) throw new CommandException(Component.text("Invalid index"));

                DatabasePlugin.executorService.submit(() -> bot.playerFilter.remove(player.playerName));

                return Component.translatable(
                        "Removed %s from the filters",
                        Component.text(player.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.executorService.submit(() -> bot.playerFilter.clear());
                return Component.text("Cleared the filter").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (FilteredPlayer player : PlayerFilterPlugin.localList) {
                    Component options = Component.empty().color(NamedTextColor.DARK_GRAY);

                    if (player.ignoreCase || player.regex) {
                        final List<Component> args = new ArrayList<>();

                        if (player.ignoreCase) args.add(Component.text("ignore case"));
                        if (player.regex) args.add(Component.text("regex"));

                        options = options
                                .append(Component.text("("))
                                .append(
                                        Component
                                                .join(
                                                        JoinConfiguration.commas(true),
                                                        args
                                                )
                                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                                )
                                .append(Component.text(")"))
                                .append(Component.space());
                    }

                    if (!player.reason.isEmpty()) {
                        options = options
                                .append(Component.text("("))
                                .append(Component.text("reason: ").color(NamedTextColor.GRAY))
                                .append(
                                        Component
                                                .text(player.reason)
                                                .color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                                )
                                .append(Component.text(")"));
                    }

                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s %s",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(player.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                                    options
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Filtered players ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(PlayerFilterPlugin.localList.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }
}
