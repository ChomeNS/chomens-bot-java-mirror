package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class WhitelistCommand extends Command {
    public WhitelistCommand () {
        super(
                "whitelist",
                "Manages whitelist",
                new String[] { "enable", "disable", "add <player>", "remove <index>", "clear", "list" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, true);

        switch (action) {
            case "enable" -> {
                bot.whitelist.enable();

                return Component.text("Enabled whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "disable" -> {
                bot.whitelist.disable();

                return Component.text("Disabled whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "add" -> {
                final String player = context.getString(true, true);

                bot.whitelist.add(player);

                return Component.translatable(
                        "Added %s to the whitelist",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "remove" -> {
                final String player = context.getString(true, true);

                if (!bot.whitelist.list.contains(player)) throw new CommandException(Component.text("Player doesn't exist in the list"));

                if (player.equals(bot.profile.getName())) throw new CommandException(Component.text("Cannot remove the bot"));

                bot.whitelist.remove(player);

                return Component.translatable(
                        "Removed %s from the whitelist",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "clear" -> {
                bot.whitelist.clear();

                return Component.text("Cleared the whitelist").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "list" -> {
                final List<Component> playersComponent = new ArrayList<>();

                int index = 0;
                for (String player : bot.whitelist.list) {
                    playersComponent.add(
                            Component.translatable(
                                    "%s › %s",
                                    Component.text(index).color(ColorUtilities.getColorByString(bot.config.colorPalette.number)),
                                    Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                            ).color(NamedTextColor.DARK_GRAY)
                    );

                    index++;
                }

                return Component.empty()
                        .append(Component.text("Whitelisted players ").color(NamedTextColor.GREEN))
                        .append(Component.text("(").color(NamedTextColor.DARK_GRAY))
                        .append(Component.text(bot.whitelist.list.size()).color(NamedTextColor.GRAY))
                        .append(Component.text(")").color(NamedTextColor.DARK_GRAY))
                        .append(Component.newline())
                        .append(
                                Component.join(JoinConfiguration.newlines(), playersComponent)
                        );
            }
            default -> throw new CommandException(Component.text("Invalid action"));
        }
    }
}
