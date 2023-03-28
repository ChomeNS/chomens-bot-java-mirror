package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrepLogCommand implements Command {
    public String name() { return "greplog"; }

    public String description() {
        return "Queries the bot's log files";
    }

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

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] _args, String[] fullArgs) {
        final Bot bot = context.bot();

        String[] args = _args;

        boolean ignoreCase = false;
        boolean regex = false;

        switch (_args[0]) {
            case "-ignorecase" -> {
                ignoreCase = true;
                args = Arrays.copyOfRange(_args, 1, _args.length);
            }
            case "-regex" -> {
                regex = true;
                args = Arrays.copyOfRange(_args, 1, _args.length);
            }
            case "stop" -> {
                bot.grepLog().thread().interrupt();
                return Component.text("success");
            }
        }

        bot.grepLog().query(String.join(" ", args), regex, ignoreCase);

        return Component.text("success");
    }
}
