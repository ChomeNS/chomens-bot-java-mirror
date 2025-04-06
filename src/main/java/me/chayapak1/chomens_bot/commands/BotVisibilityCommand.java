package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BotVisibilityCommand extends Command {
    public BotVisibilityCommand () {
        super(
                "botvisibility",
                "Changes the bot's visibility",
                new String[] { "<true|false>", "<on|off>", "" },
                new String[] { "botvis", "togglevis", "togglevisibility" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final String action = context.getString(false, false, false);

        if (action.isEmpty()) {
            bot.selfCare.visible = !bot.selfCare.visible;

            final NamedTextColor greenOrGold = bot.selfCare.visible ? NamedTextColor.GREEN : NamedTextColor.GOLD;

            final String visibleOrInvisible = bot.selfCare.visible ? "visible" : "invisible";

            return Component.empty()
                    .append(Component.text("The bot's visibility is now "))
                    .append(Component.text(visibleOrInvisible).color(greenOrGold))
                    .color(bot.colorPalette.defaultColor);
        } else {
            switch (action) {
                case "on", "true" -> {
                    bot.selfCare.visible = true;

                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("visible").color(NamedTextColor.GREEN))
                            .color(bot.colorPalette.defaultColor);
                }
                case "off", "false" -> {
                    bot.selfCare.visible = false;

                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("invisible").color(NamedTextColor.GOLD))
                            .color(bot.colorPalette.defaultColor);
                }
                default -> throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
