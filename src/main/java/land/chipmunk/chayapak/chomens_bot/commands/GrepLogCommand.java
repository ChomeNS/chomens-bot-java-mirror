package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrepLogCommand extends Command {
    public String name = "greplog";

    public String description = "Queries the bot's log files";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{input}>");
        usages.add("-ignorecase <{input}>");
        usages.add("-regex <{input}>");
        usages.add("-ignorecase -regex <{input}>");
        usages.add("stop");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("logquery");
        aliases.add("greplogs");

        return aliases;
    }

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] _args, String[] fullArgs) {
        final Bot bot = context.bot();

        String[] args = _args;

        boolean ignoreCase = false;
        boolean regex = false;

        if (_args[0].equals("stop")) {
            if (bot.grepLog().thread() == null) return Component.text("No query is running").color(NamedTextColor.RED);

            bot.grepLog().thread().interrupt();
            bot.grepLog().thread(null);

            return Component.text("Log query stopped");
        }

        if (bot.grepLog().thread() != null) return Component.text("Another query is already running").color(NamedTextColor.RED);

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

        bot.grepLog().query(String.join(" ", args), regex, ignoreCase);

        return null;
    }
}
