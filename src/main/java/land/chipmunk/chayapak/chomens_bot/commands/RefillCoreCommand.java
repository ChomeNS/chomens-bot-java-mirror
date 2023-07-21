package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class RefillCoreCommand extends Command {
    public RefillCoreCommand () {
        super(
                "refillcore",
                "Refills and resets the bots command core",
                new String[] {},
                new String[] { "rc" },
                TrustLevel.PUBLIC,
false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        bot.core.reset();
        bot.core.refill();

        return null;
    }
}
