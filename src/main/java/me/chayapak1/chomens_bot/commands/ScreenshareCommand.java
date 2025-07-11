package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3i;

// this command uses english
public class ScreenshareCommand extends Command {
    public ScreenshareCommand () {
        super(
                "screenshare",
                new String[] {
                        "start <x> <y> <z>",
                        "stop",
                        "setres <width> <height>",
                        "setfps <fps>"
                },
                new String[] {},
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true, true);

        try {
            switch (action) {
                case "start" -> {
                    context.checkOverloadArgs(4);

                    final int x = context.getInteger(true);
                    final int y = context.getInteger(true);
                    final int z = context.getInteger(true);

                    bot.screenshare.start(Vector3i.from(x, y, z));

                    return Component
                            .text("Started screen sharing")
                            .color(bot.colorPalette.defaultColor);
                }
                case "stop" -> {
                    context.checkOverloadArgs(1);

                    bot.screenshare.stop();

                    return Component
                            .text("Stopped screen sharing")
                            .color(bot.colorPalette.defaultColor);
                }
                case "setres" -> {
                    context.checkOverloadArgs(3);

                    final int width = context.getInteger(true);
                    final int height = context.getInteger(true);

                    bot.screenshare.width = width;
                    bot.screenshare.height = height;

                    bot.screenshare.screen.screen = new String[width][height];

                    return Component
                            .text("Set the resolution to ")
                            .append(Component.text(width + "x" + height, bot.colorPalette.string))
                            .color(bot.colorPalette.defaultColor);
                }
                case "setfps" -> {
                    context.checkOverloadArgs(2);

                    final int fps = context.getInteger(true);

                    bot.screenshare.fps = fps;

                    return Component
                            .text("Set the FPS to ")
                            .append(Component.text(fps, bot.colorPalette.number))
                            .color(bot.colorPalette.defaultColor);
                }
                default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
            }
        } catch (final NumberFormatException e) {
            throw new CommandException(Component.text("Invalid integer"));
        }
    }
}
