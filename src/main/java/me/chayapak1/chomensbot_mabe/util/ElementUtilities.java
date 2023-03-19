package me.chayapak1.chomensbot_mabe.util;

import me.chayapak1.chomensbot_mabe.command.Command;

import java.util.List;

public class ElementUtilities {
    public static Command findCommand(List<Command> commands, String searchTerm) {
        for (Command command : commands) {
            if (command.name().equals(searchTerm) || command.alias().contains(searchTerm)) {
                return command;
            }
        }
        return null;
    }
}
