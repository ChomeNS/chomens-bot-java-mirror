package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HelpCommand implements Command {
    public String name() { return "help"; }

    public String description() {
        return "Shows the help";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("[command]");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("heko");
        aliases.add("cmds");
        aliases.add("commands");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    private Bot bot;

    private CommandContext context;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        this.bot = context.bot();
        this.context = context;
        if (args.length == 0) {
            return sendCommandList();
        } else {
            return sendUsages(context, args);
        }
    }

    public Component sendCommandList () {
        final List<Component> list = new ArrayList<>();
        list.addAll(getCommandListByTrustLevel(0));
        list.addAll(getCommandListByTrustLevel(1));
        list.addAll(getCommandListByTrustLevel(2));

        return Component.empty()
                        .append(Component.text("Commands ").color(NamedTextColor.GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Length: ").color(NamedTextColor.GRAY))
                        .append(Component.text(list.size()).color(NamedTextColor.GREEN))
                        .append(Component.text(") ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text("Public ").color(NamedTextColor.GREEN))
                        .append(Component.text("Trusted ").color(NamedTextColor.RED))
                        .append(Component.text("Admin").color(NamedTextColor.DARK_RED))
                        .append(Component.text(") - ").color(NamedTextColor.DARK_GRAY))
                        .append(Component.join(JoinConfiguration.separator(Component.space()), list));
    }

    public List<Component> getCommandListByTrustLevel(int trustLevel) {
        final List<Component> list = new ArrayList<>();

        List<String> commandNames = new ArrayList<>();

        for (Command command : bot.commandHandler().commands()) {
            if (command.trustLevel() != trustLevel) continue;

            commandNames.add(command.name());
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

    public NamedTextColor getColorByTrustLevel (int trustLevel) {
        return switch (trustLevel) {
            case 0 -> NamedTextColor.GREEN;
            case 1 -> NamedTextColor.RED;
            case 2 -> NamedTextColor.DARK_RED;
            default -> NamedTextColor.WHITE; // hm?
        };
    }

    public Component sendUsages (CommandContext context, String[] args) {
        final Bot bot = context.bot();

        final String prefix = context.prefix();

        for (Command command : bot.commandHandler().commands()) {
            if (!command.name().equals(args[0]) && !command.alias().contains(args[0])) continue;

            final String commandName = command.name();
            final List<Component> usages = new ArrayList<>();

            usages.add(
                    Component.empty()
                            .append(Component.text(prefix + commandName).color(NamedTextColor.GOLD))
                            .append(Component.text(
                                    (command.alias().size() > 0 && !command.alias().get(0).equals("")) ?
                                            " (" + String.join(", ", command.alias()) + ")" :
                                            ""
                            ))
                            .append(Component.text(" - " + command.description()).color(NamedTextColor.GRAY))
            );

            usages.add(
                    Component.empty()
                            .append(Component.text("Trust level: ").color(NamedTextColor.GREEN))
                            .append(Component.text(command.trustLevel()).color(NamedTextColor.YELLOW))
            );

            for (String usage : command.usage()) {
                usages.add(
                        Component.empty()
                                .append(Component.text(prefix + commandName).color(NamedTextColor.GOLD))
                                .append(Component.text(" "))
                                .append(Component.text(usage).color(NamedTextColor.AQUA))
                );
            }

            return Component.join(JoinConfiguration.separator(Component.newline()), usages);
        }

        return Component.text("Unknown command").color(NamedTextColor.RED);
    }
}
