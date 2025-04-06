package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.plugins.IPFilterPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IPFilterCommand extends Command {
    public IPFilterCommand () {
        super(
                "ipfilter",
                "Filters IPs",
                new String[] {
                        "add <ip> [reason]",
                        "remove <index>",
                        "clear",
                        "list"
                },
                new String[] { "filterip", "banip", "ipban" },
                TrustLevel.ADMIN,
                false
        );
    }

    // most of these codes are from cloop and greplog
    @Override
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "add" -> {
                final String ip = context.getString(false, true);
                final String reason = context.getString(true, false);

                if (IPFilterPlugin.localList.containsKey(ip)) {
                    throw new CommandException(
                            Component.translatable(
                                    "The IP %s is already in the filters",
                                    Component.text(ip)
                            )
                    );
                }

                DatabasePlugin.EXECUTOR_SERVICE.submit(() -> bot.ipFilter.add(ip, reason));

                if (reason.isEmpty()) {
                    return Component.translatable(
                            "Added %s to the filters",
                            Component.text(ip).color(bot.colorPalette.number)
                    ).color(bot.colorPalette.defaultColor);
                } else {
                    return Component.translatable(
                            "Added %s to the filters with reason %s",
                            Component.text(ip).color(bot.colorPalette.number),
                            Component.text(reason).color(bot.colorPalette.string)
                    ).color(bot.colorPalette.defaultColor);
                }
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                final int index = context.getInteger(true);

                final String targetIP = new ArrayList<>(IPFilterPlugin.localList.keySet()).get(index);

                if (targetIP == null) throw new CommandException(Component.text("Invalid index"));

                DatabasePlugin.EXECUTOR_SERVICE.submit(() -> bot.ipFilter.remove(targetIP));

                return Component.translatable(
                        "Removed %s from the filters",
                        Component.text(targetIP).color(bot.colorPalette.number)
                ).color(bot.colorPalette.defaultColor);
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.EXECUTOR_SERVICE.submit(() -> bot.ipFilter.clear());
                return Component.text("Cleared the filter").color(bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (Map.Entry<String, String> entry : IPFilterPlugin.localList.entrySet()) {
                    final String ip = entry.getKey();
                    final String reason = entry.getValue();

                    Component reasonComponent = Component.empty().color(NamedTextColor.DARK_GRAY);

                    if (!reason.isEmpty()) {
                        reasonComponent = reasonComponent
                                .append(Component.text("("))
                                .append(Component.text("reason: ").color(NamedTextColor.GRAY))
                                .append(
                                        Component
                                                .text(reason)
                                                .color(bot.colorPalette.string)
                                )
                                .append(Component.text(")"));
                    }

                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s %s",
                                    Component.text(index).color(bot.colorPalette.number),
                                    Component.text(ip).color(bot.colorPalette.username),
                                    reasonComponent
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Filtered IPs ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(IPFilterPlugin.localList.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }
}
