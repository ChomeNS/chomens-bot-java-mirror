package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.data.CommandLoop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloopCommand implements Command {
    public String name() { return "cloop"; }

    public String description() {
        return "Loop commands";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("add <interval> <{command}>");
        usages.add("remove <index>");
        usages.add("clear");
        usages.add("list");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("commandloop");

        return aliases;
    }

    public int trustLevel() {
        return 1;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        switch (args[0]) {
            case "add" -> {
                if (args.length < 3) return Component.text("Please specify interval and command").color(NamedTextColor.RED);
                int interval;
                try {
                    interval = Integer.parseInt(args[1]);
                } catch (IllegalArgumentException ignored) {
                    return Component.text("Invalid index").color(NamedTextColor.RED);
                }

                final String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                bot.cloop().add(interval, command);

                context.sendOutput(
                        Component.translatable(
                                "Added %s with interval %s to the cloops",
                                Component.text(command).color(NamedTextColor.AQUA),
                                Component.text(interval).color(NamedTextColor.GOLD)
                        )
                );
            }
            case "remove" -> {
                try {
                    final int index = Integer.parseInt(args[1]);
                    bot.cloop().remove(index);

                    context.sendOutput(
                            Component.translatable(
                                    "Removed cloop %s",
                                    Component.text(index).color(NamedTextColor.GOLD)
                            )
                    );
                } catch (IllegalArgumentException | NullPointerException ignored) {
                    return Component.text("Invalid index").color(NamedTextColor.RED);
                }
            }
            case "clear" -> {
                bot.cloop().clear();
                context.sendOutput(
                        Component.text("Cleared all cloops")
                );
            }
            case "list" -> {
                final List<Component> cloopsComponent = new ArrayList<>();

                int index = 0;
                for (CommandLoop command : bot.cloop().loops()) {
                    cloopsComponent.add(
                            Component.translatable(
                                    "%s › %s (%s)",
                                    Component.text(index).color(NamedTextColor.GREEN),
                                    Component.text(command.command()).color(NamedTextColor.AQUA),
                                    Component.text(command.interval()).color(NamedTextColor.GOLD)
                            ).color(NamedTextColor.DARK_GRAY)
                    );
                    index++;
                }

                final Component component = Component.empty()
                        .append(Component.text("Cloops ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.cloop().loops().size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), cloopsComponent)
                        );

                context.sendOutput(component);
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }

        return Component.text("success");
    }
}
