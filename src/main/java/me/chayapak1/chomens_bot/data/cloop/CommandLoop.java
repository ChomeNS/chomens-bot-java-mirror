package me.chayapak1.chomens_bot.data.cloop;

import java.time.temporal.ChronoUnit;

public record CommandLoop (
    String command,
    int interval,
    ChronoUnit unit
) {}

