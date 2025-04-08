package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class EndCommand extends Command {
    public EndCommand () {
        super(
                "end",
                "Reconnects the bot",
                new String[] { "" },
                new String[] { "reconnect" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        bot.session.disconnect("End command");

        return null;
    }
}
