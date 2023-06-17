package land.chipmunk.chayapak.chomens_bot.commands;

import com.github.ricksbrown.cowsay.plugin.CowExecutor;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class CowsayCommand implements Command {
    public String name() { return "cowsay"; }

    public String description() {
        return "Moo";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{message}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.PUBLIC;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final String message = String.join(" ", args);

        final CowExecutor cowExecutor = new CowExecutor();
        cowExecutor.setMessage(message);

        final String result = cowExecutor.execute();

        return Component.text(result);
    }
}
