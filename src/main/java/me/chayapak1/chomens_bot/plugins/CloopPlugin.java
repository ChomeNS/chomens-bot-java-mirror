package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.cloop.CommandLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CloopPlugin {
    private final Bot bot;

    private final List<ScheduledFuture<?>> loopTasks = new ArrayList<>();
    public final List<CommandLoop> loops = new ArrayList<>();

    public CloopPlugin (final Bot bot) {
        this.bot = bot;
    }

    public void add (final TimeUnit unit, final long interval, final String command) {
        final Runnable loopTask = () -> bot.core.run(command);

        loops.add(new CommandLoop(command, interval, unit));
        loopTasks.add(bot.executor.scheduleAtFixedRate(loopTask, 0, interval, unit));
    }

    public CommandLoop remove (final int index) {
        final ScheduledFuture<?> loopTask = loopTasks.remove(index);

        if (loopTask != null) {
            loopTask.cancel(false);
        }

        return loops.remove(index);
    }

    public void clear () {
        for (final ScheduledFuture<?> loopTask : loopTasks) {
            loopTask.cancel(false);
        }

        loopTasks.clear();

        loops.clear();
    }
}
