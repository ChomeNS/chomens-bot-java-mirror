package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ValidateCommand extends Command {
    public ValidateCommand () {
        super(
                "validate",
                "Validates a hash",
                new String[] { "" },
                new String[] { "checkhash" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String[] fullArgs = context.fullArgs;

        if (fullArgs.length == 0) return null;

        final String hash = fullArgs[0];

        if (bot.hashing.isCorrectHash(hash, context.userInputCommandName, context.sender)) return Component.text("Valid hash").color(NamedTextColor.GREEN);
        else if (bot.hashing.isCorrectOwnerHash(hash, context.userInputCommandName, context.sender)) return Component.text("Valid OwnerHash").color(NamedTextColor.GREEN);

        return null;
    }
}
