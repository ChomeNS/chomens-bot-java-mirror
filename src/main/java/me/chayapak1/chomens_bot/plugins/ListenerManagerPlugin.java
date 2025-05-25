package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class ListenerManagerPlugin {
    private final Bot bot;

    private final List<Listener> listeners = Collections.synchronizedList(new ObjectArrayList<>());

    public ListenerManagerPlugin (final Bot bot) {
        this.bot = bot;
    }

    public void dispatch (final Consumer<Listener> consumer) {
        synchronized (listeners) {
            for (final Listener listener : listeners) {
                try {
                    consumer.accept(listener);
                } catch (final Throwable throwable) {
                    bot.logger.error(
                            Component.translatable(
                                    "Caught an error while trying to dispatch an event to %s!",
                                    Component.text(listener.getClass().getSimpleName())
                            )
                    );
                    bot.logger.error(throwable);
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
                } catch (final Throwable throwable) {
                    bot.logger.error(
                            Component.translatable(
                                    "Caught an error while trying to dispatch an event with a returning boolean to %s!",
                                    Component.text(listener.getClass().getSimpleName())
                            )
                    );
                    bot.logger.error(throwable);
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
