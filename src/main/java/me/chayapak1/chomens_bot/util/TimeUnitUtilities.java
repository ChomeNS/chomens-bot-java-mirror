package me.chayapak1.chomens_bot.util;

import org.apache.commons.lang3.tuple.Pair;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

public class TimeUnitUtilities {
    // some weird function used in cloop
    public static Pair<Long, TimeUnit> fromChronoUnit (final long interval, final ChronoUnit chronoUnit) {
        final Duration duration = chronoUnit.getDuration();

        if (duration.getSeconds() >= 1) return Pair.of(interval * duration.toSeconds(), TimeUnit.SECONDS);
        else return Pair.of(interval * duration.toNanos(), TimeUnit.NANOSECONDS);
    }
}
