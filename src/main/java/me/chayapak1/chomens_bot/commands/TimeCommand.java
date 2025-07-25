package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.TimeUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;

public class TimeCommand extends Command {
    public TimeCommand () {
        super(
                "time",
                new String[] { "<timezone>" },
                new String[] { "dateandtime", "date" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        try {
            final Bot bot = context.bot;

            final String timezone = context.getString(true, true);

            final String formattedTime = TimeUtilities.formatTime(
                    Instant.now().toEpochMilli(),
                    "EEEE, MMMM d, yyyy, hh:mm:ss a",
                    ZoneId.of(timezone)
            );

            return Component.translatable(
                    "commands.time.output",
                    bot.colorPalette.defaultColor,
                    Component.text(timezone, bot.colorPalette.string),
                    Component.text(formattedTime, NamedTextColor.GREEN)
            );
        } catch (final DateTimeException e) {
            throw new CommandException(Component.translatable("commands.time.error.invalid_timezone"));
        }
    }
}
