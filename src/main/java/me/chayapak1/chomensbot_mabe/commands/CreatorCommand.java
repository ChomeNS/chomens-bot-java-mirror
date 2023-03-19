package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CreatorCommand implements Command {
    public String name() { return "creator"; }

    public String description() {
        return "Shows the bot's creator";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

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
        context.sendOutput(
                Component.empty()
                        .append(Component.text("ChomeNS Bot ").color(NamedTextColor.YELLOW))
                        .append(Component.text("was created by ").color(NamedTextColor.WHITE))
                        .append(Component.text("chayapak").color(NamedTextColor.GREEN))
        );

        return Component.text("success");
    }
}
