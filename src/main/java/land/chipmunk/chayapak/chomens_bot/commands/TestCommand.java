package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class TestCommand implements Command {
    public String name() { return "test"; }

    public String description() {
        return "Tests if the bot is working";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[{args}]");

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
        final Component component = Component.translatable(
                "Hello, World! Username: %s, Sender UUID: %s, Prefix: %s, Args: %s",
                context.sender().profile().getName(),
                Component.text(context.sender().profile().getIdAsString()),
                Component.text(context.prefix()),
                Component.text(String.join(", ", args))
        ).color(NamedTextColor.GREEN);

        context.sendOutput(component);

        return Component.text("success");
    }
}
