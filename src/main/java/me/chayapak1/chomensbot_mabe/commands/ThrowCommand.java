package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class ThrowCommand implements Command {
    public String description() {
        return "A command to throw an error, kinda useless";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[message]");

        return usages;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) throws Exception {
        final String message = String.join(" ", args);

        throw new Exception(message.equals("") ? "among us" : message);
    }
}
