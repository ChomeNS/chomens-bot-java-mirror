package me.chayapak1.chomens_bot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampUtilities {
    // totallynotskidded™ from SongPlayer (modified a bit)
    public static final Pattern TIMESTAMP_PATTERN = Pattern.compile("(?:(\\d+):)?(\\d+):(\\d+)");

    public static long parseTimestamp (String timestamp) {
        final Matcher matcher = TIMESTAMP_PATTERN.matcher(timestamp);

        if (!matcher.matches()) return -1;

        long time = 0;

        final String hourString = matcher.group(1);
        final String minuteString = matcher.group(2);
        final String secondString = matcher.group(3);

        if (hourString != null) time += (long) Integer.parseInt(hourString) * 60 * 60 * 1000;
        time += (long) Integer.parseInt(minuteString) * 60 * 1000;
        time += (long) (Double.parseDouble(secondString) * 1000.0);

        return time;
    }
}
