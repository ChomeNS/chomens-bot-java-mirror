package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
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
