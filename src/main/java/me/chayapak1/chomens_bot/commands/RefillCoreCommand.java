package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class RefillCoreCommand implements Command {
    public String name() { return "refillcore"; }

    public String description() {
        return "Refills and resets the bot's command core";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("rc");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        bot.core().reset();
        bot.core().refill();

        return Component.text("success");
    }
}
