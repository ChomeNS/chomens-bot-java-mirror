package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.selfCares.essentials.VanishSelfCare;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class BotVisibilityCommand extends Command {
    public BotVisibilityCommand () {
        super(
                "botvisibility",
                new String[] { "<true|false>", "<on|off>", "" },
                new String[] { "botvis", "togglevis", "togglevisibility" },
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final String action = context.getString(false, false, false);

        final VanishSelfCare vanish = bot.selfCare.find(VanishSelfCare.class);

        if (action.isEmpty()) {
            vanish.visible = !vanish.visible;
            vanish.needsRunning = true;

            final NamedTextColor greenOrGold = vanish.visible ? NamedTextColor.GREEN : NamedTextColor.GOLD;

            final String visibleOrInvisible = vanish.visible ? "visible" : "invisible";

            return Component.translatable(
                    "commands.botvisibility.message",
                    bot.colorPalette.defaultColor,
                    Component.translatable("commands.botvisibility." + visibleOrInvisible, greenOrGold)
            );
        } else {
            switch (action) {
                case "on", "true" -> {
                    vanish.visible = true;
                    vanish.needsRunning = true;

                    return Component.translatable(
                            "commands.botvisibility.message",
                            bot.colorPalette.defaultColor,
                            Component.translatable("commands.botvisibility.visible", NamedTextColor.GREEN)
                    );
                }
                case "off", "false" -> {
                    vanish.visible = false;
                    vanish.needsRunning = true;

                    return Component.translatable(
                            "commands.botvisibility.message",
                            bot.colorPalette.defaultColor,
                            Component.translatable("commands.botvisibility.invisible", NamedTextColor.GOLD)
                    );
                }
                default -> throw new CommandException(Component.translatable("commands.generic.error.invalid_action"));
            }
        }
    }
}
