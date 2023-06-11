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

public class TPSBarCommand implements Command {
    public String name() { return "tpsbar"; }

    public String description() {
        return "Shows the server's TPS using Minecraft Bossbar";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<on|off>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("tps");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.PUBLIC;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        switch (args[0]) {
            case "on" -> {
                bot.tps().on();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("enabled").color(NamedTextColor.GREEN))
                        .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
            }
            case "off" -> {
                bot.tps().off();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("disabled").color(NamedTextColor.RED))
                        .color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }
    }
}
