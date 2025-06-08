package me.chayapak1.chomens_bot.data.cloop;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;

public record CommandLoop(
        String command,
        long interval,
        ChronoUnit unit,
        ScheduledFuture<?> task
) { }

