package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BotVisibilityCommand extends Command {
    public BotVisibilityCommand () {
        super(
                "botvisibilty",
                "Changes the bot's visibility",
                new String[] { "<true|false>", "<on|off>", "" },
                new String[] { "botvis", "togglevis", "togglevisibility" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String action = context.getString(false, false);

        if (action.isEmpty()) {
            final boolean visibility = bot.selfCare.visibility;
            bot.selfCare.visibility = !visibility;

            final NamedTextColor greenOrGold = bot.selfCare.visibility ? NamedTextColor.GREEN : NamedTextColor.GOLD;
            final String visibleOrInvisible = bot.selfCare.visibility ? "visible" : "invisible";
            final String disableOrEnable = bot.selfCare.visibility ? "disable" : "enable";
            bot.core.run("/essentials:vanish " + bot.profile.getName() + " " + disableOrEnable);
            return Component.empty()
                    .append(Component.text("The bot's visibility is now "))
                    .append(Component.text(visibleOrInvisible).color(greenOrGold))
                    .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
        } else {
            switch (action) {
                case "on", "true" -> {
                    bot.selfCare.visibility = true;
                    bot.core.run("/essentials:vanish " + bot.profile.getName() + " disable");
                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("visible").color(NamedTextColor.GREEN))
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                }
                case "off", "false" -> {
                    bot.selfCare.visibility = false;
                    bot.core.run("/essentials:vanish " + bot.profile.getName() + " enable");
                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("invisible").color(NamedTextColor.GOLD))
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
                }
                default -> {
                    throw new CommandException(Component.text("Invalid action"));
                }
            }
        }
    }
}
