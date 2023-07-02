package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.CommandLoop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CloopPlugin {
    private final Bot bot;

    private final List<ScheduledFuture<?>> loopTasks = new ArrayList<>();
    public final List<CommandLoop> loops = new ArrayList<>();

    public CloopPlugin (Bot bot) {
        this.bot = bot;
    }

    public void add (int interval, String command) {
        Runnable loopTask = () -> bot.core.run(command);

        loops.add(new CommandLoop(command, interval)); // mabe,.,..
        // should i use 50 or 0?
        loopTasks.add(bot.executor.scheduleAtFixedRate(loopTask, 0, interval, TimeUnit.MILLISECONDS));
    }

    public void remove (int index) {
        ScheduledFuture<?> loopTask = loopTasks.remove(index);

        if (loopTask != null) {
            loopTask.cancel(true);
        }

        loops.remove(index);
    }

    public void clear () {
        for (ScheduledFuture<?> loopTask : loopTasks) {
            loopTask.cancel(true);
        }

        loopTasks.clear();

        loops.clear();
    }
}
