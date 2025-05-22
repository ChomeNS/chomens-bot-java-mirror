package me.chayapak1.chomens_bot.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtilities {
    public static String formatTime (final long milliseconds, final String format, final ZoneId zoneId) {
        final Instant instant = Instant.ofEpochMilli(milliseconds);
        final OffsetDateTime localDateTime = OffsetDateTime.ofInstant(instant, zoneId);

        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        return localDateTime.format(formatter);
    }
}
