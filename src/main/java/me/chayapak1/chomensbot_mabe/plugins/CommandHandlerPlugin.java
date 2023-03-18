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
import java.util.List;
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
        registerCommand("validate", new ValidateCommand());
    }

    public void registerCommand (String commandName, Command command) {
        commands.put(commandName, command);
    }

    public static Component executeCommand (String input, CommandContext context, String hash, String ownerHash) {
        final String[] splitInput = input.split("\\s+");

        final String commandName = splitInput[0];

        final Command command = commands.get(commandName);

        // idea told this as "Condition 'command == null' is always 'false'" and its not true LMFAO
        if (command == null) return Component.text("Unknown command: " + commandName).color(NamedTextColor.RED);

        final int trustLevel = command.trustLevel();

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);
        final int longestUsageIndex = getLongestUsageIndex(command.usage());
        final int minimumArgs = getMinimumArgs(command.usage().get(longestUsageIndex));
        final int maximumArgs = getMaximumArgs(command.usage().get(longestUsageIndex));
        if (fullArgs.length < minimumArgs) return Component.text("Excepted minimum of " + minimumArgs + " argument(s), got " + fullArgs.length).color(NamedTextColor.RED);
        if (fullArgs.length > maximumArgs) return Component.text("Too much arguments, expected " + maximumArgs).color(NamedTextColor.RED);

        String userHash = "";
        if (trustLevel > 0 && splitInput.length >= 2) userHash = splitInput[1];

        final String[] args = Arrays.copyOfRange(splitInput, (trustLevel > 0) ? 2 : 1, splitInput.length);

        if (command.trustLevel() > 0) {
            if (
                    command.trustLevel() == 1 &&
                            !userHash.equals(hash) &&
                            !userHash.equals(ownerHash)
            ) return Component.text("Invalid hash").color(NamedTextColor.RED);

            if (
                    command.trustLevel() == 2 &&
                            !userHash.equals(ownerHash)
            ) return Component.text("Invalid OwnerHash").color(NamedTextColor.RED);
        }

        try {
            return command.execute(context, args, fullArgs);
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
    }

    private static int getLongestUsageIndex(List<String> usages) {
        int longestIndex = 0;
        int maxLength = 0;
        for (int i = 0; i < usages.size(); i++) {
            String[] args = usages.get(i).split("\\s+");
            if (args.length > maxLength) {
                longestIndex = i;
                maxLength = args.length;
            }
        }
        return longestIndex;
    }

    private static int getMinimumArgs(String usage) {
        int count = 0;
        for (int i = 0; i < usage.length(); i++) {
            if (usage.charAt(i) == '<') {
                count++;
            }
        }
        return count;
    }

    private static int getMaximumArgs(String usage) {
        int count = 0;
        for (int i = 0; i < usage.length(); i++) {
            if (usage.charAt(i) == '<' || usage.charAt(i) == '[') {
                count++;
            }
        }
        return count;
    }
}
