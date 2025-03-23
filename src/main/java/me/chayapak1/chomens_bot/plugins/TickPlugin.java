package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TickPlugin {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public TickPlugin (Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void tick () {
        for (Listener listener : listeners) {
            try {
                listener.onAlwaysTick();
            } catch (Exception e) {
                bot.logger.error("Caught exception in an always tick listener!");
                bot.logger.error(e);
            }
        }

        if (!bot.loggedIn) return;

        for (Listener listener : listeners) {
            try {
                listener.onTick();
            } catch (Exception e) {
                bot.logger.error("Caught exception in a tick listener!");
                bot.logger.error(e);
            }
        }
    }

    public void addListener (Listener listener) {
        listeners.add(listener);
    }

    public interface Listener {
        default void onTick () {}
        default void onAlwaysTick () {}
    }
}
