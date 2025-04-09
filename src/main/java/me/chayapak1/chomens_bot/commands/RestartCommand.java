package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class RestartCommand extends Command {
    public RestartCommand () {
        super(
                "restart",
                "Gracefully restarts the bot",
                new String[] { "[reason]" },
                new String[] {},
                TrustLevel.OWNER
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String reason = context.getString(true, false);

        Main.stop(12, reason.isEmpty() ? null : reason, "Restarting..");

        return Component.text("Restarting").color(bot.colorPalette.defaultColor);
    }
}
