package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.*;
import me.chayapak1.chomens_bot.data.filter.FilteredPlayer;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.plugins.PlayerFilterPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FilterCommand extends Command {
    public FilterCommand () {
        super(
                "filter",
                new String[] {
                        "add <player> [reason]",
                        "-ignorecase add <player> [reason]",
                        "-regex add <player> [reason]",
                        "remove <index>",
                        "clear",
                        "list"
                },
                new String[] { "filterplayer", "ban", "blacklist" },
                TrustLevel.ADMIN
        );
    }

    // most of these codes are from cloop and greplog
    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final List<String> flags = context.getFlags(true, CommonFlags.IGNORE_CASE, CommonFlags.REGEX);

        final boolean ignoreCase = flags.contains(CommonFlags.IGNORE_CASE);
        final boolean regex = flags.contains(CommonFlags.REGEX);

        final String action = context.getString(false, true, true);

        switch (action) {
            case "add" -> {
                final String player = context.getString(false, true);
                final String reason = context.getString(true, false);

                if (
                        PlayerFilterPlugin.localList.stream()
                                .map(FilteredPlayer::playerName)
                                .toList()
                                .contains(player)
                ) {
                    throw new CommandException(
                            Component.translatable(
                                    "commands.filter.add.error.already_exists",
                                    Component.text(player)
                            )
                    );
                }

                if (regex) {
                    // try validating the regex
                    try {
                        Pattern.compile(player);
                    } catch (final PatternSyntaxException e) {
                        throw new CommandException(
                                Component.translatable(
                                        "commands.filter.error.invalid_regex",
                                        Component.text(e.toString())
                                )
                        );
                    }
                }

                DatabasePlugin.EXECUTOR_SERVICE.execute(() -> bot.playerFilter.add(player, reason, regex, ignoreCase));

                if (reason.isEmpty()) {
                    return Component.translatable(
                            "commands.filter.add.no_reason",
                            bot.colorPalette.defaultColor,
                            Component.text(player, bot.colorPalette.username)
                    );
                } else {
                    return Component.translatable(
                            "commands.filter.add.reason",
                            bot.colorPalette.defaultColor,
                            Component.text(player, bot.colorPalette.username),
                            Component.text(reason, bot.colorPalette.string)
                    );
                }
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                final int index = context.getInteger(true);

                try {
                    final FilteredPlayer player = PlayerFilterPlugin.localList.get(index);

                    if (player == null) throw new IllegalArgumentException();

                    DatabasePlugin.EXECUTOR_SERVICE.execute(() -> bot.playerFilter.remove(player.playerName()));

                    return Component.translatable(
                            "commands.filter.remove.output",
                            bot.colorPalette.defaultColor,
                            Component.text(player.playerName(), bot.colorPalette.username)
                    );
                } catch (final IndexOutOfBoundsException | IllegalArgumentException | NullPointerException e) {
                    throw new CommandException(Component.translatable("commands.generic.error.invalid_index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.EXECUTOR_SERVICE.execute(bot.playerFilter::clear);
                return Component.translatable("commands.filter.clear.output", bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (final FilteredPlayer player : PlayerFilterPlugin.localList) {
                    Component options = Component.empty().color(NamedTextColor.DARK_GRAY);

                    if (player.ignoreCase() || player.regex()) {
                        final List<Component> args = new ArrayList<>();

                        if (player.ignoreCase()) args.add(Component.translatable("commands.filter.list.ignore_case"));
                        if (player.regex()) args.add(Component.translatable("commands.filter.list.regex"));

                        options = options
                                .append(Component.text("("))
                                .append(
                                        Component
                                                .join(
                                                        JoinConfiguration.commas(true),
                                                        args
                                                )
                                                .color(bot.colorPalette.string)
                                )
                                .append(Component.text(")"))
                                .append(Component.space());
                    }

                    if (!player.reason().isEmpty()) {
                        options = options
                                .append(Component.text("("))
                                .append(
                                        Component.translatable(
                                                "commands.filter.list.reason",
                                                NamedTextColor.GRAY,
                                                Component.text(player.reason(), bot.colorPalette.string)
                                        )
                                )
                                .append(Component.text(")"));
                    }

                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s %s",
                                    NamedTextColor.DARK_GRAY,
                                    Component.text(index, bot.colorPalette.number),
                                    Component.text(player.playerName(), bot.colorPalette.username),
                                    options
                            )
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.translatable("commands.filter.list.filtered_players_text", NamedTextColor.GREEN))
                        .append(Component.text("(", NamedTextColor.DARK_GRAY))
                        .append(Component.text(PlayerFilterPlugin.localList.size(), NamedTextColor.GRAY))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }
    }
}
