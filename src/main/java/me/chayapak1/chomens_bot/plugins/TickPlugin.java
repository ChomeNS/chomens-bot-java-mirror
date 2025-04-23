package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TickPlugin {
    private final Bot bot;

    public final AtomicLong lastTickTime = new AtomicLong();
    public final AtomicLong lastSecondTickTime = new AtomicLong();

    public TickPlugin (final Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(this::tickSecond, 0, 1, TimeUnit.SECONDS);
    }

    private void tick () {
        bot.listener.dispatch(listener -> {
            try {
                listener.onAlwaysTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in an always tick listener!");
                bot.logger.error(e);
            }
        });

        if (!bot.loggedIn) return;

        bot.listener.dispatch(listener -> {
            try {
                listener.onTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a tick listener!");
                bot.logger.error(e);
            }
        });

        lastTickTime.set(System.currentTimeMillis());
    }

    private void tickSecond () {
        if (!bot.loggedIn) return;

        bot.listener.dispatch(listener -> {
            try {
                listener.onSecondTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a second tick listener!");
                bot.logger.error(e);
            }
        });

        lastSecondTickTime.set(System.currentTimeMillis());
    }
}
