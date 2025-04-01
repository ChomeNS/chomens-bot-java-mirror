package me.chayapak1.chomens_bot.data.cloop;

import java.util.concurrent.TimeUnit;

public record CommandLoop(
        String command,
        long interval,
        TimeUnit unit
) { }

