package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.cloop.CommandLoop;
import me.chayapak1.chomens_bot.util.TimeUnitUtilities;
import org.apache.commons.lang3.tuple.Pair;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CloopPlugin {
    private final Bot bot;

    public final List<CommandLoop> loops = new ObjectArrayList<>();

    public CloopPlugin (final Bot bot) {
        this.bot = bot;
    }

    public void add (final ChronoUnit unit, final long interval, final String command) {
        final Pair<Long, TimeUnit> converted = TimeUnitUtilities.fromChronoUnit(interval, unit);

        final long convertedInterval = converted.getLeft();
        final TimeUnit timeUnit = converted.getRight();

        loops.add(
                new CommandLoop(
                        command,
                        interval,
                        unit,
                        bot.executor.scheduleAtFixedRate(
                                () -> bot.core.run(command),
                                0,
                                convertedInterval,
                                timeUnit
                        )
                )
        );
    }

    public CommandLoop remove (final int index) {
        final CommandLoop removed = loops.remove(index);
        removed.task().cancel(false);
        return removed;
    }

    public void clear () {
        for (final CommandLoop loop : loops) {
            loop.task().cancel(false);
        }

        loops.clear();
    }
}
