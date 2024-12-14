package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.FilteredPlayer;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.plugins.FilterPlugin;
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
                        "add <player>",
                        "-ignorecase add <player>",
                        "-regex add <player>",
                        "-ignorecase -regex add <player>",
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
                final String player = context.getString(true, true);

                final boolean finalRegex = regex;
                final boolean finalIgnoreCase = ignoreCase;

                DatabasePlugin.executorService.submit(() -> bot.filter.add(player, finalRegex, finalIgnoreCase));
                return Component.translatable(
                        "Added %s to the filters",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                final int index = context.getInteger(true);

                final FilteredPlayer player = FilterPlugin.localList.get(index);

                if (player == null) throw new CommandException(Component.text("Invalid index"));

                DatabasePlugin.executorService.submit(() -> bot.filter.remove(player.playerName));

                return Component.translatable(
                        "Removed %s from the filters",
                        Component.text(player.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.executorService.submit(() -> bot.filter.clear());
                return Component.text("Cleared the filter").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (FilteredPlayer player : FilterPlugin.localList) {
                    Component options = Component.empty().color(NamedTextColor.DARK_GRAY);

                    if (player.ignoreCase || player.regex) {
                        final List<Component> args = new ArrayList<>();

                        if (player.ignoreCase) args.add(Component.text("ignore case"));
                        if (player.regex) args.add(Component.text("regex"));

                        options = options.append(Component.text("("));
                        options = options.append(Component.join(JoinConfiguration.commas(true), args).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)));
                        options = options.append(Component.text(")"));
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
                        .append(Component.text(FilterPlugin.localList.size()).color(NamedTextColor.GRAY))
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
