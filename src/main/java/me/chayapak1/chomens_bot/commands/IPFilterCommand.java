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
                new String[] {
                        "add <ip> [reason]",
                        "remove <index>",
                        "clear",
                        "list"
                },
                new String[] { "filterip", "banip", "ipban" },
                TrustLevel.ADMIN
        );
    }

    // most of these codes are from cloop and greplog
    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getAction();

        switch (action) {
            case "add" -> {
                final String ip = context.getString(false, true);
                final String reason = context.getString(true, false);

                if (IPFilterPlugin.localList.containsKey(ip)) {
                    throw new CommandException(
                            Component.translatable(
                                    "commands.ipfilter.add.error.already_exists",
                                    Component.text(ip)
                            )
                    );
                }

                DatabasePlugin.EXECUTOR_SERVICE.execute(() -> bot.ipFilter.add(ip, reason));

                if (reason.isEmpty()) {
                    return Component.translatable(
                            "commands.filter.add.no_reason",
                            bot.colorPalette.defaultColor,
                            Component.text(ip).color(bot.colorPalette.username)
                    );
                } else {
                    return Component.translatable(
                            "commands.filter.add.reason",
                            bot.colorPalette.defaultColor,
                            Component.text(ip).color(bot.colorPalette.username),
                            Component.text(reason).color(bot.colorPalette.string)
                    );
                }
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                final int index = context.getInteger(true);

                final String targetIP = new ArrayList<>(IPFilterPlugin.localList.keySet()).get(index);

                if (targetIP == null) throw new CommandException(Component.translatable("commands.generic.error.invalid_index"));

                DatabasePlugin.EXECUTOR_SERVICE.execute(() -> bot.ipFilter.remove(targetIP));

                return Component.translatable(
                        "commands.ipfilter.remove.output",
                        bot.colorPalette.defaultColor,
                        Component.text(targetIP, bot.colorPalette.username)
                );
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                DatabasePlugin.EXECUTOR_SERVICE.execute(bot.ipFilter::clear);
                return Component.translatable("commands.ipfilter.clear.output").color(bot.colorPalette.defaultColor);
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (final Map.Entry<String, String> entry : IPFilterPlugin.localList.entrySet()) {
                    final String ip = entry.getKey();
                    final String reason = entry.getValue();

                    Component reasonComponent = Component.empty().color(NamedTextColor.DARK_GRAY);

                    if (!reason.isEmpty()) {
                        reasonComponent = reasonComponent
                                .append(Component.text("("))
                                .append(
                                        Component.translatable(
                                                "commands.ipfilter.list.reason",
                                                NamedTextColor.GRAY,
                                                Component.text(reason, bot.colorPalette.string)
                                        )
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
                        .append(Component.translatable("commands.ipfilter.list.filtered_ips_text", NamedTextColor.GREEN))
                        .append(Component.text("(", NamedTextColor.DARK_GRAY))
                        .append(Component.text(IPFilterPlugin.localList.size(), NamedTextColor.GRAY))
                        .append(Component.text(")", NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
        }
    }
}
