package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
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
        usages.add("<hash> <{c:message|command}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("ayunsudo");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.TRUSTED;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        for (MutablePlayerListEntry entry : bot.players().list()) {
            bot.core().run("essentials:sudo " + entry.profile().getName() + " " + String.join(" ", args));
        }

        return null;
    }
}
