package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends Command {
    public UptimeCommand () {
        super(
                "uptime",
                "Shows the bots uptime",
                new String[] {},
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

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
        ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
