package me.chayapak1.chomensbot_mabe.commands;

import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
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

    public int trustLevel() {
        return 1;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        if (args[0] == null) {
            final boolean visibility = bot.selfCare().visibility();
            bot.selfCare().visibility(!visibility);

            final NamedTextColor greenOrGold = bot.selfCare().visibility() ? NamedTextColor.GREEN : NamedTextColor.GOLD;
            final String visibleOrInvisible = bot.selfCare().visibility() ? "visible" : "invisible";
            final String disableOrEnable = bot.selfCare().visibility() ? "disable" : "enable";
            bot.chat().send("/essentials:vanish " + disableOrEnable);
            context.sendOutput(
                    Component.empty()
                            .append(Component.text("The bot's visibility is now "))
                            .append(Component.text(visibleOrInvisible).color(greenOrGold))
            );
        } else {
            switch (args[0]) {
                case "on", "true" -> {
                    bot.selfCare().visibility(true);
                    context.sendOutput(
                            Component.empty()
                                    .append(Component.text("The bot's visibility is now "))
                                    .append(Component.text("visible").color(NamedTextColor.GREEN))
                    );
                }
                case "off", "false" -> {
                    bot.selfCare().visibility(false);
                    context.sendOutput(
                            Component.empty()
                                    .append(Component.text("The bot's visibility is now "))
                                    .append(Component.text("invisible").color(NamedTextColor.GOLD))
                    );
                }
                default -> {

                }
            }
        }

        return Component.text("success");
    }
}
