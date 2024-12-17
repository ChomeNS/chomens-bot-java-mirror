package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

public class RestartCommand extends Command {
    public RestartCommand() {
        super(
                "restart",
                "Gracefully restarts the bot",
                new String[] { "" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        Main.stop(1);

        return Component.text("Restarting").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
