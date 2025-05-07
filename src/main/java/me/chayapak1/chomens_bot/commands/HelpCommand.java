package me.chayapak1.chomens_bot.commands;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HelpCommand extends Command {
    public HelpCommand () {
        super(
                "help",
                new String[] { "[command]" },
                new String[] { "heko", "cmds", "commands" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final String commandName = context.getString(false, false);

        if (commandName.isBlank()) {
            return getCommandList(context);
        } else {
            return getUsages(context, commandName);
        }
    }

    public Component getCommandList (final CommandContext context) {
        final List<Component> list = new ObjectArrayList<>();

        for (final TrustLevel level : TrustLevel.values()) {
            list.addAll(getCommandListByTrustLevel(context, level));
        }

        final Component trustLevels = Component.join(
                JoinConfiguration.spaces(),
                Arrays.stream(TrustLevel.values())
                        .map(level -> level.component)
                        .toList()
        );

        return Component.empty()
                .append(Component.translatable("commands.help.commands_text").color(NamedTextColor.GRAY))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                .append(Component.text(") ").color(NamedTextColor.DARK_GRAY))
                .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                .append(Component.translatable("%s", trustLevels))
                .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                .append(Component.join(JoinConfiguration.separator(Component.space()), list));
    }

    public List<Component> getCommandListByTrustLevel (final CommandContext context, final TrustLevel trustLevel) {
        final Bot bot = context.bot;

        final List<Component> list = new ObjectArrayList<>();

        final List<String> commandNames = new ObjectArrayList<>();

        for (final Command command : CommandHandlerPlugin.COMMANDS) {
            if (command.trustLevel != trustLevel || (command.consoleOnly && !(context instanceof ConsoleCommandContext)))
                continue;

            commandNames.add(command.name);
        }

        Collections.sort(commandNames);

        for (final String name : commandNames) {
            final String clickSuggestion = context.prefix + name; // *command
            final String insertionSuggestion = context.prefix + this.name + " " + name; // *help <command>
            list.add(
                    Component
                            .text(name)
                            .color(trustLevel.component.color())
                            .clickEvent(
                                    ClickEvent.suggestCommand(clickSuggestion)
                            )
                            .insertion(insertionSuggestion)
                            .hoverEvent(
                                    HoverEvent.showText(
                                            Component.empty()
                                                    .color(NamedTextColor.GREEN)
                                                    .append(
                                                            Component.translatable(
                                                                    "commands.help.hover.click_to_command",
                                                                    Component.text(clickSuggestion, bot.colorPalette.string)
                                                            )
                                                    )
                                                    .append(Component.newline())
                                                    .append(
                                                            Component.translatable(
                                                                    "commands.help.hover.shift_click_to_help_command",
                                                                    Component.text(insertionSuggestion, bot.colorPalette.string)
                                                            )
                                                    )
                                    )
                            )

                    // there are too many commands and having hover being the usages will make the command length > 32767 :(
                            /* .hoverEvent(
                                    HoverEvent.showText(
                                            getUsages(context, name)
                                    )
                            ) */
            );
        }

        return list;
    }

    public Component getUsages (final CommandContext context, final String commandName) throws CommandException {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        for (final Command command : CommandHandlerPlugin.COMMANDS) {
            if (
                    !command.name.equalsIgnoreCase(commandName) &&
                            !Arrays.stream(command.aliases).toList().contains(commandName.toLowerCase())
            ) continue;

            final String actualCommandName = command.name.toLowerCase();
            final List<Component> usages = new ObjectArrayList<>();

            usages.add(
                    Component.empty()
                            .color(NamedTextColor.GRAY)
                            .append(Component.text(prefix + actualCommandName).color(bot.colorPalette.secondary))
                            .append(
                                    Component
                                            .text(
                                                    (command.aliases.length > 0 && !command.aliases[0].isEmpty()) ?
                                                            " (" + String.join(", ", command.aliases) + ")" :
                                                            "",
                                                    NamedTextColor.WHITE
                                            )
                            )
                            .append(Component.text(" - "))
                            .append(
                                    Component.translatable(
                                            String.format(
                                                    "commands.%s.description",
                                                    actualCommandName
                                            )
                                    )
                            )
            );

            usages.add(
                    Component.empty()
                            .append(Component.translatable("commands.help.trust_level").color(NamedTextColor.GREEN))
                            .append(
                                    command.trustLevel.component
                                            .append(Component.text(" - "))
                                            .append(Component.text(command.trustLevel.level))
                            )
            );

            for (final String usage : command.usages) {
                Component usageComponent = Component.empty()
                        .append(Component.text(prefix + actualCommandName).color(bot.colorPalette.secondary))
                        .append(Component.text(" "));

                usageComponent = usageComponent.append(Component.text(usage).color(bot.colorPalette.string));

                usages.add(usageComponent);
            }

            return Component.join(JoinConfiguration.separator(Component.newline()), usages);
        }

        throw new CommandException(Component.translatable("commands.help.error.unknown_command"));
    }
}
