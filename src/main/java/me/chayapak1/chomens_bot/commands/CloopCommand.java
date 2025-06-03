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

                final ChronoUnit unit = context.getEnum(true, ChronoUnit.class);

                if (unit == ChronoUnit.NANOS && interval < 1000)
                    throw new CommandException(Component.translatable("commands.cloop.add.error.too_low_nanoseconds"));

                final String command = context.getString(true, true);

                try {
                    bot.cloop.add(unit, interval, command);
                } catch (final Exception e) {
                    throw new CommandException(Component.text(e.toString()));
                }

                return Component.translatable(
                        "commands.cloop.add.output",
                        bot.colorPalette.defaultColor,
                        Component.text(command, bot.colorPalette.string),
                        Component.text(interval, bot.colorPalette.number),
                        Component.text(unit.toString(), bot.colorPalette.string)
                );
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                try {
                    final int index = context.getInteger(true);

                    final CommandLoop cloop = bot.cloop.remove(index);

                    return Component.translatable(
                            "commands.cloop.remove.output",
                            bot.colorPalette.defaultColor,
                            Component.text(cloop.command(), bot.colorPalette.string)
                    );
                } catch (final IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.translatable("commands.generic.error.invalid_index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.cloop.clear();
                return Component.translatable("commands.cloop.clear.output", bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> cloopsComponent = new ArrayList<>();

                int index = 0;
                for (final CommandLoop command : bot.cloop.loops) {
                    cloopsComponent.add(
                            Component.translatable(
                                    "%s › %s (%s %s)",
                                    NamedTextColor.DARK_GRAY,
                                    Component.text(index, bot.colorPalette.number),
                                    Component.text(command.command(), bot.colorPalette.string),
                                    Component.text(command.interval(), bot.colorPalette.number),
                                    Component.text(command.unit().toString(), bot.colorPalette.string)
                            )
                    );
                    index++;
                }

                return Component.empty()
                        .append(Component.translatable("commands.cloop.list.cloops_text", NamedTextColor.GREEN))
                        .append(Component.text("(", NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.cloop.loops.size(), NamedTextColor.GRAY))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), cloopsComponent)
                        );
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }
    }
}
