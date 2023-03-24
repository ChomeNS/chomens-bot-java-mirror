package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class EchoCommand implements Command {
    public String name() { return "echo"; }

    public String description() {
        return "Says a message";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{message}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("say");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        bot.chat().send(String.join(" ", args));

        return Component.text("success");
    }
}
