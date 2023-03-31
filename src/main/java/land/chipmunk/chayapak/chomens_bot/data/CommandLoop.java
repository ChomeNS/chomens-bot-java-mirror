package land.chipmunk.chayapak.chomens_bot.data;

import lombok.Getter;

public class CommandLoop {
    @Getter
    private String command;
    @Getter
    private int interval;

    public CommandLoop (String command, int interval) {
        this.command = command;
        this.interval = interval;
    }
}

