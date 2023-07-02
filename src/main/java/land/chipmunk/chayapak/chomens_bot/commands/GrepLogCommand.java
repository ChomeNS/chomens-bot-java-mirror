package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

public class GrepLogCommand extends Command {
    public GrepLogCommand () {
        super(
                "greplog",
                "Queries the bots log files",
                new String[] {
                        "<{input}>",
                        "-ignorecase <{input}>",
                        "-regex <{input}>",
                        "-ignorecase -regex <{input}>",
                        "stop"
                },
                new String[] { "logquery", "greplogs" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] _args, String[] fullArgs) {
        final Bot bot = context.bot;

        String[] args = _args;

        boolean ignoreCase = false;
        boolean regex = false;

        if (_args[0].equals("stop")) {
            bot.grepLog.thread.interrupt();
            bot.grepLog.thread = null;

            return Component.text("Log query stopped");
        }

        if (bot.grepLog.thread != null) return Component.text("Another query is already running").color(NamedTextColor.RED);

        // this is a mess
        if (_args[0].equals("-ignorecase")) {
            ignoreCase = true;
            args = Arrays.copyOfRange(_args, 1, _args.length);
        } else if (_args[0].equals("-regex")) {
            regex = true;
            args = Arrays.copyOfRange(_args, 1, _args.length);
        }

        if (_args.length > 1 && _args[1].equals("-ignorecase")) {
            ignoreCase = true;
            args = Arrays.copyOfRange(_args, 2, _args.length);
        } else if (_args.length > 1 && _args[1].equals("-regex")) {
            regex = true;
            args = Arrays.copyOfRange(_args, 2, _args.length);
        }

        bot.grepLog.query(String.join(" ", args), regex, ignoreCase);

        return null;
    }
}
