package me.chayapak1.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.FilteredPlayer;
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
                TrustLevel.OWNER,
                false
        );
    }

    // most of these codes are from cloop and greplog
    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        boolean ignoreCase = false;
        boolean regex = false;

        String action = context.getString(false, true);

        // this is a mess
        if (action.equals("-ignorecase")) {
            ignoreCase = true;
            action = context.getString(false, true);
        } else if (action.equals("-regex")) {
            regex = true;
            action = context.getString(false, true);
        }

        if (action.equals("-ignorecase")) {
            ignoreCase = true;
            action = context.getString(false, true);
        } else if (action.equals("-regex")) {
            regex = true;
            action = context.getString(false, true);
        }

        final Gson gson = new Gson();

        switch (action) {
            case "add" -> {
                final String player = context.getString(true, true);

                bot.filter.add(player, regex, ignoreCase);
                return Component.translatable(
                        "Added %s to the filters",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                try {
                    final int index = context.getInteger(true);

                    final FilteredPlayer removed = bot.filter.remove(index);

                    return Component.translatable(
                            "Removed %s from the filters",
                            Component.text(removed.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.filter.clear();
                return Component.text("Cleared the filter").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (JsonElement playerElement : FilterPlugin.filteredPlayers) {
                    final FilteredPlayer player = gson.fromJson(playerElement, FilteredPlayer.class);

                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(player.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Filtered players ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(FilterPlugin.filteredPlayers.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
