package land.chipmunk.chayapak.chomens_bot.command;

import net.kyori.adventure.text.Component;

public class CommandException extends Exception {
    public final Component message;

    public CommandException (Component message) {
        this.message = message;
    }
}
