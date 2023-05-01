package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TickPlugin extends Bot.Listener {
    private final Bot bot;

    private ScheduledFuture<?> tickTask;

    private final List<Listener> listeners = new ArrayList<>();

    public TickPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void connected(ConnectedEvent event) {
        tickTask = bot.executor().scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void tick () {
        for (Listener listener : listeners) {
            listener.onTick();
        }
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        tickTask.cancel(true);
    }

    public void addListener (Listener listener) {
        listeners.add(listener);
    }

    public static class Listener {
        public void onTick () {}
    }
}
