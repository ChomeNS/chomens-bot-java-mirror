package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.data.FilteredPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FilterCommand implements Command {
    public String name() { return "filter"; }

    public String description() {
        return "Filter players";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<hash> add <{player}>");
        usages.add("<hash> -ignorecase add <{player}>");
        usages.add("<hash> -regex add <{player}>");
        usages.add("<hash> -ignorecase -regex add <{player}>");
        usages.add("<hash> remove <index>");
        usages.add("<hash> clear");
        usages.add("<hash> list");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("filterplayer");

        return aliases;
    }

    public int trustLevel() {
        return 1;
    }

    // most of these codes are from cloop and greplog
    public Component execute(CommandContext context, String[] _args, String[] fullArgs) {
        final Bot bot = context.bot();

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

        switch (args[0]) {
            case "add" -> {
                final String player = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                bot.filter().add(player, regex, ignoreCase);
                return Component.translatable(
                        "Added %s to the filters",
                        Component.text(player).color(NamedTextColor.AQUA)
                );
            }
            case "remove" -> {
                try {
                    final int index = Integer.parseInt(args[1]);

                    final FilteredPlayer removed = bot.filter().remove(index);

                    return Component.translatable(
                            "Removed %s from the filters",
                            Component.text(removed.playerName).color(NamedTextColor.AQUA)
                    );
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    return Component.text("Invalid index").color(NamedTextColor.RED);
                }
            }
            case "clear" -> {
                bot.filter().clear();
                return Component.text("Cleared the filter");
            }
            case "list" -> {
                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (FilteredPlayer player : bot.filter().filteredPlayers()) {
                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(NamedTextColor.GREEN),
                                    Component.text(player.playerName).color(NamedTextColor.AQUA)
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Filtered players ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.filter().filteredPlayers().size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }
    }
}
