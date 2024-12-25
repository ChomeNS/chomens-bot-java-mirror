package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

public class StopCommand extends Command {
    public StopCommand () {
        super(
                "stop",
                "Gracefully stops the bot",
                new String[] { "[reason]" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String reason = context.getString(true, false);

        Main.stop(0, reason.isEmpty() ? null : reason);

        return Component.text("Stopping").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
