package me.chayapak1.chomensbot_mabe.plugins;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.command.Command;
import me.chayapak1.chomensbot_mabe.command.CommandContext;
import me.chayapak1.chomensbot_mabe.commands.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CommandHandlerPlugin {
    @Getter private static final Map<String, Command> commands = new HashMap<>();

    public CommandHandlerPlugin () {
        registerCommand("cb", new CommandBlockCommand());
        registerCommand("cowsay", new CowsayCommand());
        registerCommand("echo", new EchoCommand());
        registerCommand("help", new HelpCommand());
        registerCommand("test", new TestCommand());
        registerCommand("throw", new ThrowCommand());
    }

    public void registerCommand (String commandName, Command command) {
        commands.put(commandName, command);
    }

    public static Component executeCommand (String input, CommandContext context) {
        final String[] splitInput = input.split(" ");

        final String commandName = splitInput[0];
        final String[] args = Arrays.copyOfRange(splitInput, 1, splitInput.length);

        final Command command = commands.get(commandName);

        if (command != null) {
            try {
                return command.execute(context, args);
            } catch (Exception exception) {
                exception.printStackTrace();

                final String stackTrace = ExceptionUtils.getStackTrace(exception);
                return Component
                        .text("An error occurred while trying to execute the command, hover here for more details", NamedTextColor.RED)
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component
                                                .text(stackTrace)
                                                .color(NamedTextColor.RED)
                                )
                        );
            }
        } else {
            return Component.text("Unknown command: " + commandName).color(NamedTextColor.RED);
        }
    }
}
