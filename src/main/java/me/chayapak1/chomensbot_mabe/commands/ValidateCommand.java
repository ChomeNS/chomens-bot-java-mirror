package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ValidateCommand implements Command {
    public String name() { return "validate"; }

    public String description() {
        return "Validates a hash";
    }

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

    public int trustLevel() {
        return 1;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();
        final String hash = fullArgs[0];

        if (hash.equals(context.hash())) context.sendOutput(Component.text("Valid hash").color(NamedTextColor.GREEN));
        else if (hash.equals(context.ownerHash())) context.sendOutput(Component.text("Valid OwnerHash").color(NamedTextColor.GREEN));

        return Component.text("success");
    }
}
