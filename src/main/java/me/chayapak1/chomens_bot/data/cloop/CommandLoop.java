package me.chayapak1.chomens_bot.data.cloop;

import java.util.concurrent.TimeUnit;

public record CommandLoop (
    String command,
    int interval,
    TimeUnit unit
) {}

