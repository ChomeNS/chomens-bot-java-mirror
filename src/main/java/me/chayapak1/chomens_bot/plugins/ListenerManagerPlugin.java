package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListenerManagerPlugin {
    private final Bot bot;

    private final List<Listener> listeners = Collections.synchronizedList(new LinkedList<>());

    public ListenerManagerPlugin (final Bot bot) {
        this.bot = bot;
    }

    public void dispatch (final Consumer<Listener> consumer) {
        synchronized (listeners) {
            for (final Listener listener : listeners) {
                try {
                    consumer.accept(listener);
                } catch (final Exception e) {
                    bot.logger.error(
                            Component.translatable(
                                    "Caught exception while trying to dispatch an event to %s!",
                                    Component.text(listener.getClass().getSimpleName())
                            )
                    );
                    bot.logger.error(e);
                }
            }
        }
    }

    public void dispatchWithCheck (final Function<Listener, Boolean> function) {
        synchronized (listeners) {
            for (final Listener listener : listeners) {
                try {
                    final Boolean result = function.apply(listener);

                    if (result != null && !result) break;
                } catch (final Exception e) {
                    bot.logger.error(
                            Component.translatable(
                                    "Caught exception while trying to dispatch an event with a returning boolean to %s!",
                                    Component.text(listener.getClass().getSimpleName())
                            )
                    );
                    bot.logger.error(e);
                }
            }
        }
    }

    public void addListener (final Listener listener) {
        if (listeners.contains(listener)) throw new IllegalArgumentException(
                "This listener is already in the listeners list. " +
                        "Please call `removeListener(listener)` first."
        );
        listeners.add(listener);
    }

    public void removeListener (final Listener listener) {
        listeners.remove(listener);
    }
}
