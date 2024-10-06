package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeCommand extends Command {
    public TimeCommand () {
        super(
                "time",
                "Shows the date and time for the specified timezone",
                new String[] { "<timezone>" },
                new String[] { "dateandtime", "date" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String timezone = context.getString(true, true);

        DateTimeZone zone;
        try {
            zone = DateTimeZone.forID(timezone);
        } catch (IllegalArgumentException ignored) {
            throw new CommandException(Component.text("Invalid timezone (case-sensitive)"));
        }

        final DateTime dateTime = new DateTime(zone);

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE, MMMM d, YYYY, hh:mm:ss a");
        final String formattedTime = formatter.print(dateTime);

        return Component.translatable(
                "The current time for %s is: %s",
                Component.text(timezone).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                Component.text(formattedTime).color(NamedTextColor.GREEN)
        ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
