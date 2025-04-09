package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TickPlugin {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public TickPlugin (final Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(this::tickSecond, 0, 1, TimeUnit.SECONDS);
    }

    private void tick () {
        for (final Listener listener : listeners) {
            try {
                listener.onAlwaysTick();
            } catch (final Exception e) {
                bot.logger.error("Caught exception in an always tick listener!");
                bot.logger.error(e);
            }
        }

        if (!bot.loggedIn) return;

        for (final Listener listener : listeners) {
            try {
                listener.onTick();
            } catch (final Exception e) {
                bot.logger.error("Caught exception in a tick listener!");
                bot.logger.error(e);
            }
        }
    }

    private void tickSecond () {
        if (!bot.loggedIn) return;

        for (final Listener listener : listeners) {
            try {
                listener.onSecondTick();
            } catch (final Exception e) {
                bot.logger.error("Caught exception in a second tick listener!");
                bot.logger.error(e);
            }
        }
    }

    public void addListener (final Listener listener) {
        listeners.add(listener);
    }

    public interface Listener {
        default void onTick () { }

        default void onAlwaysTick () { }

        default void onSecondTick () { }
    }
}
