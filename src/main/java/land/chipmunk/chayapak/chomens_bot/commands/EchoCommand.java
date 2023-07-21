package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class EchoCommand extends Command {
    public EchoCommand () {
        super(
                "echo",
                "Makes the bot say a message",
                new String[] { "<{message}>" },
                new String[] { "say" },
                TrustLevel.PUBLIC,
false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        bot.chat.send(String.join(" ", args));

        return null;
    }
}
