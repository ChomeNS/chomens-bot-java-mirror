package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import me.chayapak1.chomensbot_mabe.plugins.CommandHandlerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommand implements Command {
    public String description() {
        return "Shows the help";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[command]");

        return usages;
    }

    public Component execute (CommandContext context, String[] args) {
        if (args.length == 0) {
            sendCommandList(context);
            return Component.text("success");
        } else {
            return sendUsages(context, args);
        }
    }

    public void sendCommandList(CommandContext context) {
        final List<Component> list = new ArrayList<>();

        for (Map.Entry<String, Command> entry : CommandHandlerPlugin.commands().entrySet()) {
            final String name = entry.getKey();

            list.add(Component.text(name));
        }

        final Component component = Component.empty()
                        .append(Component.text("Commands ").color(NamedTextColor.GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Length: ").color(NamedTextColor.GRAY))
                        .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                        .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.join(JoinConfiguration.separator(Component.space()), list).color(NamedTextColor.GREEN));

        context.sendOutput(component);
    }

    public Component sendUsages (CommandContext context, String[] args) {
        final Bot bot = context.bot();

        final String prefix = bot.chatCommandHandler().prefix();

        final String commandName = args[0];

        for (Map.Entry<String, Command> entry : CommandHandlerPlugin.commands().entrySet()) {
            if (!entry.getKey().equals(commandName)) continue;

            final List<Component> usages = new ArrayList<>();

            final Command command = entry.getValue();

            usages.add(
                    Component.empty()
                            .append(Component.text(prefix + commandName).color(NamedTextColor.GOLD))
                            .append(Component.text(" - " + command.description()).color(NamedTextColor.GRAY))
            );

            usages.add(
                    Component.empty()
                            .append(Component.text("Trust level: ").color(NamedTextColor.GREEN))
                            .append(Component.text("TODO").color(NamedTextColor.YELLOW))
            );

            for (String usage : command.usage()) {
                usages.add(
                        Component.empty()
                                .append(Component.text(prefix + commandName).color(NamedTextColor.GOLD))
                                .append(Component.text(" "))
                                .append(Component.text(usage).color(NamedTextColor.AQUA))
                );
            }

            context.sendOutput(Component.join(JoinConfiguration.separator(Component.newline()), usages));

            return Component.text("success");
        }

        return Component.text("Unknown command").color(NamedTextColor.RED);
    }
}
