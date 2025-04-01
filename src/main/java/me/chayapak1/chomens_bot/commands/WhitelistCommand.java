package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand extends Command {
    public WhitelistCommand () {
        super(
                "whitelist",
                "Manages whitelist",
                new String[] { "enable", "disable", "add <player>", "remove <index>", "clear", "list" },
                new String[] {},
                TrustLevel.ADMIN,
                false
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "enable" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.enable();

                return Component.text("Enabled whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "disable" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.disable();

                return Component.text("Disabled whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "add" -> {
                final String player = context.getString(true, true);

                bot.whitelist.add(player);

                return Component.translatable(
                        "Added %s to the whitelist",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                try {
                    final int index = context.getInteger(true);

                    final String player = bot.whitelist.remove(index);

                    return Component.translatable(
                            "Removed %s from the whitelist",
                            Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.whitelist.clear();

                return Component.text("Cleared the whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> playersComponent = new ArrayList<>();

                int index = 0;
                for (String player : bot.whitelist.list) {
                    playersComponent.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Whitelisted players ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.whitelist.list.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), playersComponent)
                        );
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }
}
