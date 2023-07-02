package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ClearChatQueueCommand extends Command {
    public ClearChatQueueCommand () {
        super(
                "clearchatqueue",
                "Clears the bots chat queue",
                new String[] {},
                new String[] { "ccq" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        bot.chat.clearQueue();

        return null;
    }
}
