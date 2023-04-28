package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.ricksbrown.cowsay.plugin.CowExecutor;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CowsayCommand extends Command {
    public String name = "cowsay";

    public String description = "Moo";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<cow> <{message}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final String cow = args[0];
        final String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        final CowExecutor cowExecutor = new CowExecutor();
        cowExecutor.setCowfile(cow);
        cowExecutor.setMessage(message);

        final String result = cowExecutor.execute();

        return Component.text(result);
    }
}
