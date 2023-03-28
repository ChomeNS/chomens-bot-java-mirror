package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class BruhifyCommand implements Command {
    public String name() { return "bruhify"; }

    public String description() {
        return "RecycleBot bruhify but actionbar";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[{message}]");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        if (args.length == 0) {
            bot.bruhify().bruhifyText("");
        } else {
            bot.bruhify().bruhifyText(String.join(" ", args));
        }

        return Component.text("success");
    }
}
