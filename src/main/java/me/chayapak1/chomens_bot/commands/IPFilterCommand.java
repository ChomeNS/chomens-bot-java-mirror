package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.plugins.IPFilterPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class IPFilterCommand extends Command {
    public IPFilterCommand() {
        super(
                "ipfilter",
                "Filter IPs",
                new String[] {
                        "add <ip>",
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
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true, true);

        switch (action) {
            case "add" -> {
                final String ip = context.getString(true, true);

                bot.ipFilter.add(ip);
                return Component.translatable(
                        "Added %s to the filters",
                        Component.text(ip).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                context.checkOverloadArgs(2);

                try {
                    final int index = context.getInteger(true);

                    final String removed = bot.ipFilter.remove(index);

                    return Component.translatable(
                            "Removed %s from the filters",
                            Component.text(removed).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                    ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NullPointerException ignored) {
                    throw new CommandException(Component.text("Invalid index"));
                }
            }
            case "clear" -> {
                context.checkOverloadArgs(1);

                bot.ipFilter.clear();
                return Component.text("Cleared the filter").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                context.checkOverloadArgs(1);

                final List<Component> filtersComponents = new ArrayList<>();

                int index = 0;
                for (JsonNode playerElement : IPFilterPlugin.filteredIPs.deepCopy()) {
                    filtersComponents.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(playerElement.asText()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Filtered IPs ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(IPFilterPlugin.filteredIPs.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), filtersComponents)
                        );
            }
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
