package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.FilteredPlayer;
import land.chipmunk.chayapak.chomens_bot.plugins.FilterPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterCommand extends Command {
    public FilterCommand () {
        super(
                "filter",
                "Filter players",
                new String[] {
                        "<hash> add <{player}>",
                        "<hash> -ignorecase add <{player}>",
                        "<hash> -regex add <{player}>",
                        "<hash> -ignorecase -regex add <{player}>",
                        "<hash> remove <index>",
                        "<hash> clear",
                        "<hash> list"
                },
                new String[] { "filterplayer", "ban", "blacklist" },
                TrustLevel.TRUSTED,
                false
        );
    }

    // most of these codes are from cloop and greplog
    public Component execute(CommandContext context, String[] _args, String[] fullArgs) {
        final Bot bot = context.bot;

        boolean ignoreCase = false;
        boolean regex = false;

        String[] args = _args;

        // this is a mess
        if (_args[0].equals("-ignorecase")) {
            ignoreCase = true;
            args = Arrays.copyOfRange(_args, 1, _args.length);
        } else if (_args[0].equals("-regex")) {
            regex = true;
            args = Arrays.copyOfRange(_args, 1, _args.length);
        }

        if (_args.length > 1 && _args[1].equals("-ignorecase")) {
            ignoreCase = true;
            args = Arrays.copyOfRange(_args, 2, _args.length);
        } else if (_args.length > 1 && _args[1].equals("-regex")) {
            regex = true;
            args = Arrays.copyOfRange(_args, 2, _args.length);
        }

        final Gson gson = new Gson();

        switch (args[0]) {
            case "add" -> {
                final String player = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                bot.filter.add(player, regex, ignoreCase);
                return Component.translatable(
                        "Added %s to the filters",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                try {
                    final int index = Integer.parseInt(args[1]);

                    final FilteredPlayer removed = bot.filter.remove(index);

                    return Component.translatable(
                            "Removed %s from the filters",
                            Component.text(removed.playerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    return Component.text("Invalid index").color(NamedTextColor.RED);
                }
            }
            case "clear" -> {
                bot.filter.clear();
                return Component.text("Cleared the filter").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
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
                return Component.text("Invalid action").color(NamedTextColor.RED);
            }
        }
    }
}
