package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class RefillCoreCommand extends Command {
    public RefillCoreCommand () {
        super(
                "refillcore",
                "Refills and resets the bots command core",
                new String[] {},
                new String[] { "rc" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        bot.core.reset();
        bot.core.refill();

        return Component
                .text("Refilled the command core")
                .color(bot.colorPalette.defaultColor);
    }
}
