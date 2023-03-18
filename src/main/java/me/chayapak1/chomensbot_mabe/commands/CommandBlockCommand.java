package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class CommandBlockCommand implements Command {
    public String description() {
        return "Executes a command in the command core";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<command>");

        return usages;
    }

    public Component execute(CommandContext context, String[] args) {
        final Bot bot = context.bot();

        bot.core().run(String.join(" ", args));

        return Component.text("success");
    }
}
