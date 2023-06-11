package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class BotVisibilityCommand implements Command {
    public String name() { return "botvisibility"; }

    public String description() {
        return "Changes the bot's visibility";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<hash> <true|false>");
        usages.add("<hash> <on|off>");
        usages.add("<hash>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("botvis");
        aliases.add("togglevis");
        aliases.add("togglevisibility");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.TRUSTED;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        if (args.length == 0) {
            final boolean visibility = bot.selfCare().visibility();
            bot.selfCare().visibility(!visibility);

            final NamedTextColor greenOrGold = bot.selfCare().visibility() ? NamedTextColor.GREEN : NamedTextColor.GOLD;
            final String visibleOrInvisible = bot.selfCare().visibility() ? "visible" : "invisible";
            final String disableOrEnable = bot.selfCare().visibility() ? "disable" : "enable";
            bot.chat().send("/essentials:vanish " + disableOrEnable);
            return Component.empty()
                    .append(Component.text("The bot's visibility is now "))
                    .append(Component.text(visibleOrInvisible).color(greenOrGold));
        } else {
            switch (args[0]) {
                case "on", "true" -> {
                    bot.selfCare().visibility(true);
                    bot.chat().send("/essentials:vanish disable");
                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("visible").color(NamedTextColor.GREEN))
                            .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
                }
                case "off", "false" -> {
                    bot.selfCare().visibility(false);
                    bot.chat().send("/essentials:vanish enable");
                    return Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text("invisible").color(NamedTextColor.GOLD))
                            .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
                }
                default -> {
                    return Component.text("Invalid argument").color(NamedTextColor.RED);
                }
            }
        }
    }
}
