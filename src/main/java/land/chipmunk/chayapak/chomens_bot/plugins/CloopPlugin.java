package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.CommandLoop;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CloopPlugin {
    private final Bot bot;

    // too lazy to use executor
    private final List<TimerTask> loopTasks = new ArrayList<>();
    @Getter private final List<CommandLoop> loops = new ArrayList<>();

    private final Timer timer;

    public CloopPlugin (Bot bot) {
        this.bot = bot;
        this.timer = new Timer();
    }

    public void add (int interval, String command) {
        TimerTask loopTask = new TimerTask() {
            public void run() {
                bot.core().run(command);
            }
        };
        loopTasks.add(loopTask);
        loops.add(new CommandLoop(command, interval)); // mabe,.,..
        // should i use 50 or 0?
        timer.scheduleAtFixedRate(loopTask, 0, interval);
    }

    public void remove (int index) {
        TimerTask loopTask = loopTasks.remove(index);
        if (loopTask != null) {
            loopTask.cancel();
        }

        loops.remove(index);
    }

    public void clear () {
        for (TimerTask loopTask : loopTasks) {
            loopTask.cancel();
        }
        loopTasks.clear();

        loops.clear();
    }
}
