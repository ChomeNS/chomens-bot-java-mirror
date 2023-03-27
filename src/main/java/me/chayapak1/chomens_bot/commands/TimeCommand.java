package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class TimeCommand implements Command {
    public String name() { return "time"; }

    public String description() {
        return "Shows the date and time for the specified timezone";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<timezone>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("dateandtime");
        aliases.add("date");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final String timezone = args[0];

        DateTimeZone zone;
        try {
            zone = DateTimeZone.forID(timezone);
        } catch (IllegalArgumentException ignored) {
            return Component.text("Invalid timezone (case-sensitive)").color(NamedTextColor.RED);
        }

        final DateTime dateTime = new DateTime(zone);

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE, MMMM d, YYYY, hh:mm:ss a");
        final String formattedTime = formatter.print(dateTime);

        context.sendOutput(
                Component.translatable(
                        "The current date and time for the timezone %s is: %s",
                        Component.text(timezone).color(NamedTextColor.AQUA),
                        Component.text(formattedTime).color(NamedTextColor.GREEN)
                )
        );

        return Component.text("success");
    }
}
