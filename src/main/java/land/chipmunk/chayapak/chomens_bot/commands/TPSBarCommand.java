package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class TPSBarCommand extends Command {
    public String name = "tpsbar";

    public String description = "Shows the server's TPS using Minecraft Bossbar";

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

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        switch (args[0]) {
            case "on" -> {
                bot.tps().on();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("enabled").color(NamedTextColor.GREEN));
            }
            case "off" -> {
                bot.tps().off();
                return Component.empty()
                        .append(Component.text("TPSBar is now "))
                        .append(Component.text("disabled").color(NamedTextColor.RED));
            }
            default -> {
                return Component.text("Invalid argument").color(NamedTextColor.RED);
            }
        }
    }
}
