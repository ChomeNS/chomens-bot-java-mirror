package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ValidateCommand extends Command {
    public ValidateCommand () {
        super(
                "validate",
                "Validates a hash",
                new String[] { "<hash|ownerHash>" },
                new String[] { "checkhash" },
                TrustLevel.TRUSTED,
false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        final String hash = fullArgs[0];

        if (hash.equals(bot.hashing.getHash(context.splitInput[0], context.sender))) return Component.text("Valid hash").color(NamedTextColor.GREEN);
        else if (hash.equals(bot.hashing.getOwnerHash(context.splitInput[0], context.sender))) return Component.text("Valid OwnerHash").color(NamedTextColor.GREEN);

        return null;
    }
}
