package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.CommandLoop;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
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

        final String action = context.getString(false, true, true);

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
                try {
                    final int index = context.getInteger(true);
                    bot.cloop.remove(index);

                    return Component.translatable(
                            "Removed cloop %s",
                            Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                bot.cloop.clear();
                return Component.text("Cleared all cloops").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
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
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
