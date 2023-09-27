package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
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

        final String hash = context.fullArgs[0];

        if (bot.hashing.isCorrectHash(hash, context.commandName, context.sender)) return Component.text("Valid hash").color(NamedTextColor.GREEN);
        else if (bot.hashing.isCorrectOwnerHash(hash, context.commandName, context.sender)) return Component.text("Valid OwnerHash").color(NamedTextColor.GREEN);

        return null;
    }
}
