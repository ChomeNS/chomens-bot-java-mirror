package land.chipmunk.chayapak.chomens_bot.util;

import land.chipmunk.chayapak.chomens_bot.command.Command;

import java.util.List;

// Author: ChatGPT I guess
public class ElementUtilities {
    public static Command findCommand(List<Command> commands, String searchTerm) {
        for (Command command : commands) {
            if (
                    (
                        command.name().equals(searchTerm.toLowerCase()) ||
                        command.alias().contains(searchTerm.toLowerCase())
                    ) &&
                    !searchTerm.equals("") // ig yup
            ) {
                return command;
            }
        }
        return null;
    }
}
