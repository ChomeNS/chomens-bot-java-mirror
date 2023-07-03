package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;

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
        for (Listener listener : listeners) listener.onAlwaysTick();

        if (!bot.loggedIn) return;

        for (Listener listener : listeners) listener.onTick();
    }

    public void addListener (Listener listener) {
        listeners.add(listener);
    }

    public static class Listener {
        public void onTick () {}
        public void onAlwaysTick () {}
    }
}
