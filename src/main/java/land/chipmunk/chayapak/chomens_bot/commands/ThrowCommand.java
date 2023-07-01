package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

public class ThrowCommand extends Command {
    public ThrowCommand () {
        super(
                "throw",
                "Throws a Java Exception, kinda useless",
                new String[] { "[{message}]" },
                new String[] { "throwerror" },
                TrustLevel.PUBLIC
        );
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception {
        final String message = String.join(" ", args);

        throw new Exception(message.equals("") ? "among us" : message);
    }
}
