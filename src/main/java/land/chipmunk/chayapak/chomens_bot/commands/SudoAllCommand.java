package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

// ayunsudo renamed.
public class SudoAllCommand implements Command {
    public String name() { return "sudoall"; }

    public String description() {
        return "Sudoes everyone";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{c:message|command}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("ayunsudo");

        return aliases;
    }

    public int trustLevel() {
        return 1;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        for (MutablePlayerListEntry entry : bot.players().list()) {
            bot.core().run("essentials:sudo " + entry.profile().getName() + " " + String.join(" ", args));
        }

        return Component.text("success");
    }
}
