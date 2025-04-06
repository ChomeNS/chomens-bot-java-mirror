package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.ConsoleCommandContext;
import me.chayapak1.chomens_bot.plugins.CommandHandlerPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HelpCommand extends Command {
    public HelpCommand () {
        super(
                "help",
                "Shows a command list or usage for a command",
                new String[] { "[command]" },
                new String[] { "heko", "cmds", "commands" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final String commandName = context.getString(false, false);

        if (commandName.isBlank()) {
            return getCommandList(context);
        } else {
            return getUsages(context, commandName);
        }
    }

    public Component getCommandList (CommandContext context) throws CommandException {
        final List<Component> list = new ArrayList<>();

        for (TrustLevel level : TrustLevel.values()) {
            list.addAll(getCommandListByTrustLevel(context, level));
        }

        final Component trustLevels = Component.join(
                JoinConfiguration.spaces(),
                Arrays.stream(TrustLevel.values())
                        .map(level -> level.component)
                        .toList()
        );

        return Component.empty()
                .append(Component.text("Commands ").color(NamedTextColor.GRAY))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                .append(Component.text(") ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s", trustLevels))
                .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.join(JoinConfiguration.separator(Component.space()), list));
    }

    public List<Component> getCommandListByTrustLevel (CommandContext context, TrustLevel trustLevel) throws CommandException {
        final List<Component> list = new ArrayList<>();

        List<String> commandNames = new ArrayList<>();

        for (Command command : CommandHandlerPlugin.COMMANDS) {
            if (command.trustLevel != trustLevel || (command.consoleOnly && !(context instanceof ConsoleCommandContext)))
                continue;

            commandNames.add(command.name);
        }

        Collections.sort(commandNames);

        for (String name : commandNames) {
            list.add(
                    Component
                            .text(name)
                            .color(trustLevel.component.color())
                            .clickEvent(
                                    ClickEvent.suggestCommand(context.prefix + name) // *command
                            )
                            .insertion(context.prefix + this.name + " " + name) // *help <command>
                            .hoverEvent(
                                    HoverEvent.showText(
                                            getUsages(context, name)
                                    )
                            )
            );
        }

        return list;
    }

    public Component getUsages (CommandContext context, String commandName) throws CommandException {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        for (Command command : CommandHandlerPlugin.COMMANDS) {
            if (
                    !command.name.equalsIgnoreCase(commandName) &&
                            !Arrays.stream(command.aliases).toList().contains(commandName.toLowerCase())
            ) continue;

            final String actualCommandName = command.name.toLowerCase();
            final List<Component> usages = new ArrayList<>();

            usages.add(
                    Component.empty()
                            .append(Component.text(prefix + actualCommandName).color(bot.colorPalette.secondary))
                            .append(Component.text(
                                    (command.aliases.length > 0 && !command.aliases[0].isEmpty()) ?
                                            " (" + String.join(", ", command.aliases) + ")" :
                                            ""
                            ).color(NamedTextColor.WHITE))
                            .append(Component.text(" - " + command.description)).color(NamedTextColor.GRAY)
            );

            usages.add(
                    Component.empty()
                            .append(Component.text("Trust level: ").color(NamedTextColor.GREEN))
                            .append(
                                    command.trustLevel.component
                                            .append(Component.text(" - "))
                                            .append(Component.text(command.trustLevel.level))
                            )
            );

            for (String usage : command.usages) {
                Component usageComponent = Component.empty()
                        .append(Component.text(prefix + actualCommandName).color(bot.colorPalette.secondary))
                        .append(Component.text(" "));

                usageComponent = usageComponent.append(Component.text(usage).color(bot.colorPalette.string));

                usages.add(usageComponent);
            }

            return Component.join(JoinConfiguration.separator(Component.newline()), usages);
        }

        throw new CommandException(Component.text("Unknown command"));
    }
}
