package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import net.kyori.adventure.text.Component;

// ayunsudo renamed.
public class SudoAllCommand extends Command {
    public SudoAllCommand () {
        super(
                "sudoall",
                "Sudoes everyone",
                new String[] { "<hash> <{c:message|command}>" },
                new String[] { "ayunsudo" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        for (MutablePlayerListEntry entry : bot.players().list()) {
            bot.core().run("essentials:sudo " + entry.profile().getName() + " " + String.join(" ", args));
        }

        return null;
    }
}
