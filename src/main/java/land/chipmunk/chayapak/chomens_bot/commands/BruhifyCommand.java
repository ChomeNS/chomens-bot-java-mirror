package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class BruhifyCommand extends Command {
    public BruhifyCommand () {
        super(
                "bruhify",
                "RecycleBots bruhify but actionbar",
                new String[] { "[{message}]" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        if (args.length == 0) {
            bot.bruhify.bruhifyText = "";
        } else {
            bot.bruhify.bruhifyText = String.join(" ", args);
        }

        return null;
    }
}
