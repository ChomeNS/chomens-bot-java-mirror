package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.plugins.CommandHandlerPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
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
                TrustLevel.PUBLIC
        );
    }

    private Bot bot;

    private CommandContext context;

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        this.bot = context.bot;
        this.context = context;

        if (args.length == 0) {
            return sendCommandList();
        } else {
            return sendUsages(context, args);
        }
    }

    public Component sendCommandList () {
        final List<Component> list = new ArrayList<>();
        list.addAll(getCommandListByTrustLevel(TrustLevel.PUBLIC));
        list.addAll(getCommandListByTrustLevel(TrustLevel.TRUSTED));
        list.addAll(getCommandListByTrustLevel(TrustLevel.OWNER));

        return Component.empty()
                        .append(Component.text("Commands ").color(NamedTextColor.GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                        .append(Component.text(") ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Public ").color(NamedTextColor.GREEN))
                        .append(Component.text("Trusted ").color(NamedTextColor.RED))
                        .append(Component.text("Owner").color(NamedTextColor.DARK_RED))
                        .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.join(JoinConfiguration.separator(Component.space()), list));
    }

    public List<Component> getCommandListByTrustLevel(TrustLevel trustLevel) {
        final List<Component> list = new ArrayList<>();

        List<String> commandNames = new ArrayList<>();

        for (Command command : CommandHandlerPlugin.commands) {
            if (command.trustLevel != trustLevel) continue;

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
                                            sendUsages(context, new String[] { name })
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
            case OWNER -> NamedTextColor.DARK_RED;
        };
    }

    public Component sendUsages (CommandContext context, String[] args) {
        final Bot bot = context.bot;

        final String prefix = context.prefix;

        for (Command command : CommandHandlerPlugin.commands) {
            if (!command.name.equals(args[0]) && !Arrays.stream(command.aliases).toList().contains(args[0])) continue;

            final String commandName = command.name;
            final List<Component> usages = new ArrayList<>();

            usages.add(
                    Component.empty()
                            .append(Component.text(prefix + commandName).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                            .append(Component.text(
                                    (command.aliases.length > 0 && !command.aliases[0].equals("")) ?
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
                usages.add(
                        Component.empty()
                                .append(Component.text(prefix + commandName).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary)))
                                .append(Component.text(" "))
                                .append(Component.text(usage).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)))
                );
            }

            return Component.join(JoinConfiguration.separator(Component.newline()), usages);
        }

        return Component.text("Unknown command").color(NamedTextColor.RED);
    }
}
