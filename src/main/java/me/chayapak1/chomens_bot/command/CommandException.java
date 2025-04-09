package me.chayapak1.chomens_bot.command;

import net.kyori.adventure.text.Component;

public class CommandException extends Exception {
    public final Component message;

    public CommandException (final Component message) {
        this.message = message;
    }
}
