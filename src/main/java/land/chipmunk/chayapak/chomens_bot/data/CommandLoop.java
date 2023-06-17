package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class CommandLoop {
    @Getter private String command;
    @Getter private int interval;
}

