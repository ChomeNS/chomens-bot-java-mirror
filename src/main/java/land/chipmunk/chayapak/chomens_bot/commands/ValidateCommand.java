package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ValidateCommand extends Command {
    public String name = "validate";

    public String description = "Validates a hash";

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<hash|ownerHash>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("checkhash");

        return aliases;
    }

    public int trustLevel = 1;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final String hash = fullArgs[0];

        if (hash.equals(context.hash())) return Component.text("Valid hash").color(NamedTextColor.GREEN);
        else if (hash.equals(context.ownerHash())) return Component.text("Valid OwnerHash").color(NamedTextColor.GREEN);

        return null;
    }
}
