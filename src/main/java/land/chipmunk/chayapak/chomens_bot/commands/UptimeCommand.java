package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UptimeCommand implements Command {
    public String name() { return "uptime"; }

    public String description() {
        return "Shows the bot's uptime";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.PUBLIC;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final long uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000;

        long days = TimeUnit.SECONDS.toDays(uptime);
        long hours = TimeUnit.SECONDS.toHours(uptime) - (days * 24L);
        long minutes = TimeUnit.SECONDS.toMinutes(uptime) - (TimeUnit.SECONDS.toHours(uptime) * 60);
        long seconds = TimeUnit.SECONDS.toSeconds(uptime) - (TimeUnit.SECONDS.toMinutes(uptime) * 60);

        return Component.translatable(
                "The bots uptime is: %s",
                Component.translatable(
                        "%s days, %s hours, %s minutes, %s seconds",
                        Component.text(days),
                        Component.text(hours),
                        Component.text(minutes),
                        Component.text(seconds)
                ).color(NamedTextColor.GREEN)
        ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
    }
}
