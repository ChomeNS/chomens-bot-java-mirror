package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ThrowCommand extends Command {
    public String name = "throw";

    public String description = "A command to throw an error, kinda useless";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[{message}]");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("throwerror");

        return aliases;
    }

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception {
        final String message = String.join(" ", args);

        throw new Exception(message.equals("") ? "among us" : message);
    }
}
