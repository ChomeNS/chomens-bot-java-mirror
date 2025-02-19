package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.cloop.CommandLoop;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CloopCommand extends Command {
    public CloopCommand () {
        super(
                "cloop",
                "Loop commands",
                new String[] { "add <interval> <command>", "remove <index>", "clear", "list" },
                new String[] { "commandloop" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "add" -> {
                int interval = context.getInteger(true);
                if (interval < 1) interval = 1;

                final String command = context.getString(true, true);

                bot.cloop.add(interval, command);

                return Component.translatable(
                        "Added %s with interval %s to the cloops",
                        Component.text(command).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                        Component.text(interval).color(ColorUtilities.getColorByString(bot.config.colorPalette.number))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                try {
                    final int index = context.getInteger(true);

                    final CommandLoop cloop = bot.cloop.remove(index);

                    return Component.translatable(
                            "Removed cloop %s",
                            Component.text(cloop.command()).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.cloop.clear();
                return Component.text("Cleared all cloops").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> cloopsComponent = new ArrayList<>();

                int index = 0;
                for (CommandLoop command : bot.cloop.loops) {
                    cloopsComponent.add(
                            Component.translatable(
                                    "%s › %s (%s)",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(command.command()).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                                    Component.text(command.interval()).color(ColorUtilities.getColorByString(bot.config.colorPalette.number))
                            ).color(NamedTextColor.DARK_GRAY)
                    );
                    index++;
                }

                return Component.empty()
                        .append(Component.text("Cloops ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.cloop.loops.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), cloopsComponent)
                        );
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }
}
