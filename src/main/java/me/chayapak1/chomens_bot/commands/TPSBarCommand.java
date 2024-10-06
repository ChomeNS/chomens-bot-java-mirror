package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TPSBarCommand extends Command {
    public TPSBarCommand () {
        super(
                "tpsbar",
                "Shows the server's TPS using Minecraft Bossbar",
                new String[] { "<on|off>" },
                new String[] { "tps" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(1);

        final Bot bot = context.bot;

        final String action = context.getString(false, true, true);

        switch (action) {
            case "on" -> {
                bot.tps.on();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("enabled").color(NamedTextColor.GREEN))
                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            case "off" -> {
                bot.tps.off();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("disabled").color(NamedTextColor.RED))
                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
            }
            default -> {
                throw new CommandException(Component.text("Invalid action"));
            }
        }
    }
}
