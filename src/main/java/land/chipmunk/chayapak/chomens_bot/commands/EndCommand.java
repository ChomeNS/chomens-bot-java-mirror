package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class EndCommand extends Command {
    public EndCommand () {
        super(
                "end",
                "End/Reconnects the bot",
                new String[] { "<hash>" },
                new String[] { "reconnect", "restart" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) {
        final Bot bot = context.bot;

        bot.session.disconnect("End command");

        return null;
    }
}
