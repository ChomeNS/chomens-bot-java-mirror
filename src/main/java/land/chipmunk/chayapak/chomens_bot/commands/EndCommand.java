package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class EndCommand extends Command {
    public String name = "end";

    public String description = "End/Restarts the bot";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<hash>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("restart");

        return aliases;
    }

    public int trustLevel = 1;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        bot.session().disconnect("End command");

        return null;
    }
}
