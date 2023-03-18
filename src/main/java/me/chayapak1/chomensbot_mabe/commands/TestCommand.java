package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class TestCommand implements Command {
    public String description() {
        return "Tests if the bot is working";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        context.sendOutput(
                Component.empty()
                        .append(Component.text("Hello, World! Username: "))
                        .append(context.displayName())
                        .color(NamedTextColor.GREEN)
        );

        return Component.text("success");
    }
}
