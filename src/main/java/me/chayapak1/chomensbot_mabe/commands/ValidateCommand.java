package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class ValidateCommand implements Command {

    @Override
    public String description() {
        return "Validates a hash";
    }

    @Override
    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<hash|ownerHash>");

        return usages;
    }

    @Override
    public int trustLevel() {
        return 1;
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();
        final String hash = fullArgs[0];

        if (hash.equals(bot.hashing().hash())) context.sendOutput(Component.text("Valid hash").color(NamedTextColor.GREEN));
        else if (hash.equals(bot.hashing().ownerHash())) context.sendOutput(Component.text("Valid OwnerHash").color(NamedTextColor.GREEN));

        return Component.text("success");
    }
}
