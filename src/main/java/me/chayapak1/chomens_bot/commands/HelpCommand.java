package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.plugins.CommandHandlerPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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

    private CommandContext context;

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        this.context = context;

        final String commandName = context.getString(false, false);

        if (commandName.isEmpty()) {
            return sendCommandList();
        } else {
            return sendUsages(context, commandName);
        }
    }

    public Component sendCommandList () throws CommandException {
        final List<Component> list = new ArrayList<>();
        list.addAll(getCommandListByTrustLevel(TrustLevel.PUBLIC));
        list.addAll(getCommandListByTrustLevel(TrustLevel.TRUSTED));
        list.addAll(getCommandListByTrustLevel(TrustLevel.ADMIN));
        list.addAll(getCommandListByTrustLevel(TrustLevel.OWNER));

        return Component.empty()
                        .append(Component.text("Commands ").color(NamedTextColor.GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                        .append(Component.text(") ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Public ").color(NamedTextColor.GREEN))
                        .append(Component.text("Trusted ").color(NamedTextColor.RED))
                        .append(Component.text("Admin ").color(NamedTextColor.DARK_RED))
                        .append(Component.text("Owner").color(NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.join(JoinConfiguration.separator(Component.space()), list));
    }

    public List<Component> getCommandListByTrustLevel(TrustLevel trustLevel) throws CommandException {
        final List<Component> list = new ArrayList<>();

        List<String> commandNames = new ArrayList<>();

        for (Command command : CommandHandlerPlugin.commands) {
            if (command.trustLevel != trustLevel || command.consoleOnly) continue;

            commandNames.add(command.name);
        }

        Collections.sort(commandNames);

        for (String name : commandNames) {
            list.add(
                    Component
                            .text(name)
                            .color(getColorByTrustLevel(trustLevel))
                            .hoverEvent(
                                    HoverEvent.showText(
                                            sendUsages(context, name)
                                    )
                            )
            );
        }

        return list;
    }

    public NamedTextColor getColorByTrustLevel (TrustLevel trustLevel) {
        return switch (trustLevel) {
            case PUBLIC -> NamedTextColor.GREEN;
            case TRUSTED -> NamedTextColor.RED;
            case ADMIN -> NamedTextColor.DARK_RED;
            case OWNER -> NamedTextColor.LIGHT_PURPLE;
        };
    }

    public Component sendUsages (CommandContext context, String commandName) throws CommandException {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        for (Command command : CommandHandlerPlugin.commands) {
            if (!command.name.equalsIgnoreCase(commandName) && !Arrays.stream(command.aliases).toList().contains(commandName.toLowerCase())) continue;

            final String actualCommandName = command.name.toLowerCase();
            final List<Component> usages = new ArrayList<>();

            usages.add(
                    Component.empty()
                            .append(Component.text(prefix + actualCommandName).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
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
                            .append(Component.text(command.trustLevel.name()).color(NamedTextColor.YELLOW))
            );

            for (String usage : command.usages) {
                Component usageComponent = Component.empty()
                        .append(Component.text(prefix + actualCommandName).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                        .append(Component.text(" "));

                if (command.trustLevel == TrustLevel.TRUSTED) {
                    usageComponent = usageComponent
                            .append(Component.text("<hash>"))
                            .append(Component.space())
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.string));
                } else if (command.trustLevel == TrustLevel.OWNER) {
                    usageComponent = usageComponent
                            .append(Component.text("<ownerHash>"))
                            .append(Component.space())
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.string));
                }

                usageComponent = usageComponent.append(Component.text(usage).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)));

                usages.add(usageComponent);
            }

            return Component.join(JoinConfiguration.separator(Component.newline()), usages);
        }

        throw new CommandException(Component.text("Unknown command"));
    }
}
