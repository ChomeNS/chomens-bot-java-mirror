package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class AuthCommand extends Command {
    public AuthCommand () {
        super(
                "auth",
                new String[] {},
                new String[] {},
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        context.sender.authenticatedTrustLevel = context.trustLevel;

        return Component.translatable(
                "commands.auth.output",
                bot.colorPalette.defaultColor,
                context.sender.authenticatedTrustLevel.component
        );
    }
}
