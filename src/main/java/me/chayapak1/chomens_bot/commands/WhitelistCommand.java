package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand extends Command {
    public WhitelistCommand () {
        super(
                "whitelist",
                new String[] { "enable", "disable", "add <player>", "remove <index>", "clear", "list" },
                new String[] {},
                TrustLevel.ADMIN
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "enable" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.enable();

                return Component.translatable("commands.whitelist.enable", bot.colorPalette.defaultColor);
            }
            case "disable" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.disable();

                return Component.translatable("commands.whitelist.disable", bot.colorPalette.defaultColor);
            }
            case "add" -> {
                final String player = context.getString(true, true);

                bot.whitelist.add(player);

                return Component.translatable(
                        "commands.whitelist.add",
                        bot.colorPalette.defaultColor,
                        Component.text(player, bot.colorPalette.username)
                );
            }
            case "remove" -> {
                try {
                    final int index = context.getInteger(true);

                    final String player = bot.whitelist.remove(index);

                    return Component.translatable(
                            "commands.whitelist.remove",
                            bot.colorPalette.defaultColor,
                            Component.text(player, bot.colorPalette.username)
                    );
                } catch (final IndexOutOfBoundsException | IllegalArgumentException | NullPointerException e) {
                    throw new CommandException(Component.translatable("commands.generic.error.invalid_index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.clear();

                return Component.translatable("commands.whitelist.clear", bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> playersComponent = new ArrayList<>();

                int index = 0;
                for (final String player : bot.whitelist.list) {
                    playersComponent.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(bot.colorPalette.number),
                                    Component.text(player).color(bot.colorPalette.username)
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.translatable("commands.whitelist.whitelisted_players_text").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.whitelist.list.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), playersComponent)
                        );
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }
    }
}
