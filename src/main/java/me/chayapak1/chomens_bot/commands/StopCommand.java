package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class StopCommand extends Command {
    public StopCommand () {
        super(
                "stop",
                new String[] { "[reason]" },
                new String[] {},
                TrustLevel.OWNER
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String reason = context.getString(true, false);

        new Thread(
                () -> Main.stop(0, reason.isEmpty() ? null : reason),
                "ChomeNS Bot Shutdown Thread"
        ).start();

        return Component.translatable("commands.stop.output", bot.colorPalette.defaultColor);
    }
}
