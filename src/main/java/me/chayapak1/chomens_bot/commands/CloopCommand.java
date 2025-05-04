package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.cloop.CommandLoop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class CloopCommand extends Command {
    public CloopCommand () {
        super(
                "cloop",
                "Loops commands",
                new String[] { "add <interval> <ChronoUnit> <command>", "remove <index>", "clear", "list" },
                new String[] { "commandloop" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "add" -> {
                long interval = context.getLong(true);
                if (interval < 1) interval = 1;

                final ChronoUnit unit = context.getEnum(ChronoUnit.class);

                if (unit == ChronoUnit.NANOS && interval < 1000)
                    throw new CommandException(Component.text("Interval must not be less than 1000 nanoseconds"));

                final String command = context.getString(true, true);

                try {
                    bot.cloop.add(unit, interval, command);
                } catch (final Exception e) {
                    throw new CommandException(Component.text(e.toString()));
                }

                return Component.translatable(
                        "Added %s with interval %s %s to the cloops",
                        Component.text(command).color(bot.colorPalette.string),
                        Component.text(interval).color(bot.colorPalette.number),
                        Component.text(unit.toString()).color(bot.colorPalette.string)
                ).color(bot.colorPalette.defaultColor);
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                try {
                    final int index = context.getInteger(true);

                    final CommandLoop cloop = bot.cloop.remove(index);

                    return Component.translatable(
                            "Removed cloop %s",
                            Component.text(cloop.command()).color(bot.colorPalette.string)
                    ).color(bot.colorPalette.defaultColor);
                } catch (final IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.cloop.clear();
                return Component.text("Cleared all cloops").color(bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> cloopsComponent = new ArrayList<>();

                int index = 0;
                for (final CommandLoop command : bot.cloop.loops) {
                    cloopsComponent.add(
                            Component.translatable(
                                    "%s › %s (%s %s)",
                                    Component.text(index).color(bot.colorPalette.number),
                                    Component.text(command.command()).color(bot.colorPalette.string),
                                    Component.text(command.interval()).color(bot.colorPalette.number),
                                    Component.text(command.unit().toString()).color(bot.colorPalette.string)
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
