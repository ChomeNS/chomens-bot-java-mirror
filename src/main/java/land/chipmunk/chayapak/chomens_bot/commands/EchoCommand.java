package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class EchoCommand extends Command {
    public String name = "echo";

    public String description = "Says a message";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{message}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("say");

        return aliases;
    }

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        bot.chat().send(String.join(" ", args));

        return null;
    }
}
