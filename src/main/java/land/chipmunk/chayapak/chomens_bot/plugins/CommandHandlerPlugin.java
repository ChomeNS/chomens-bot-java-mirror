package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.commands.*;
import land.chipmunk.chayapak.chomens_bot.util.ElementUtilities;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandlerPlugin {
    private final Bot bot;

    @Getter private final List<Command> commands = new ArrayList<>();

    public CommandHandlerPlugin (Bot bot) {
        this.bot = bot;

        registerCommand(new CommandBlockCommand());
        registerCommand(new CowsayCommand());
        registerCommand(new EchoCommand());
        registerCommand(new CreatorCommand());
        registerCommand(new DiscordCommand());
        registerCommand(new HelpCommand());
        registerCommand(new TestCommand());
        registerCommand(new ThrowCommand());
        registerCommand(new ValidateCommand());
        registerCommand(new MusicCommand());
        registerCommand(new RandomTeleportCommand());
        registerCommand(new BotVisibilityCommand());
        registerCommand(new TPSBarCommand());
        registerCommand(new NetMessageCommand());
        registerCommand(new RefillCoreCommand());
        registerCommand(new WikipediaCommand());
        registerCommand(new UrbanCommand());
        registerCommand(new ClearChatCommand());
        registerCommand(new ListCommand());
        registerCommand(new ServerEvalCommand());
        registerCommand(new UUIDCommand());
        registerCommand(new TimeCommand());
        registerCommand(new BruhifyCommand());
        registerCommand(new GrepLogCommand());
        registerCommand(new SudoAllCommand());
        registerCommand(new EndCommand());
        registerCommand(new CloopCommand());
        registerCommand(new WeatherCommand());
        registerCommand(new ServerInfoCommand());
        registerCommand(new BotUserCommand());
        registerCommand(new GenerateMazeCommand());
        registerCommand(new TranslateCommand());
        registerCommand(new KickCommand());
        registerCommand(new ClearChatQueueCommand());
        registerCommand(new FilterCommand());
    }

    public void registerCommand (Command command) {
        commands.add(command);
    }

    // literally the same quality as the js chomens bot
    public Component executeCommand (String input, CommandContext context, boolean inGame, boolean discord, boolean console, String hash, String ownerHash, MessageReceivedEvent event) {
        final String[] splitInput = input.split("\\s+");

        String commandName;
        try {
            commandName = splitInput[0];
        } catch (ArrayIndexOutOfBoundsException e) {
            return Component.text("Empty command").color(NamedTextColor.RED);
        }

        final Command command = ElementUtilities.findCommand(commands, commandName);

        if (command == null) return Component.text("Unknown command: " + commandName).color(NamedTextColor.RED);

        final int trustLevel = command.trustLevel();

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);
        final int longestUsageIndex = getLongestUsageIndex(command.usage());
        final String usage = command.usage().get(longestUsageIndex);
        final int minimumArgs = getMinimumArgs(usage, inGame, command.trustLevel());
        final int maximumArgs = getMaximumArgs(usage, inGame, command.trustLevel());
        if (fullArgs.length < minimumArgs) return Component.text("Excepted minimum of " + minimumArgs + " argument(s), got " + fullArgs.length).color(NamedTextColor.RED);
        if (fullArgs.length > maximumArgs && !usage.contains("{")) return Component.text("Too much arguments, expected " + maximumArgs + " max").color(NamedTextColor.RED);

        if (trustLevel > 0 && splitInput.length < 2 && inGame) return Component.text("Please provide a hash").color(NamedTextColor.RED);

        String userHash = "";
        if (trustLevel > 0 && inGame) userHash = splitInput[1];

        final String[] args = Arrays.copyOfRange(splitInput, (trustLevel > 0 && inGame) ? 2 : 1, splitInput.length);

        if (command.trustLevel() > 0 && !console) {
            if (discord) {
                final List<Role> roles = event.getMember().getRoles();

                final String trustedRoleName = bot.config().discord().trustedRoleName();
                final String adminRoleName = bot.config().discord().adminRoleName();

                if (
                        command.trustLevel() == 1 &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(trustedRoleName)) &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(adminRoleName))
                ) return Component.text("You're not in the trusted role!").color(NamedTextColor.RED);

                if (
                        command.trustLevel() == 2 &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(adminRoleName))
                ) return Component.text("You're not in the admin role!").color(NamedTextColor.RED);
            } else {
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
        }

        try {
            return command.execute(context, args, fullArgs);
        } catch (Exception exception) {
            exception.printStackTrace();

            final String stackTrace = ExceptionUtils.getStackTrace(exception);
            if (inGame) {
                return Component
                        .text("An error occurred while trying to execute the command, hover here for more details", NamedTextColor.RED)
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component
                                                .text(stackTrace)
                                                .color(NamedTextColor.RED)
                                )
                        );
            } else {
                return Component.text(stackTrace).color(NamedTextColor.RED);
            }
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

    private static int getMinimumArgs(String usage, boolean inGame, int trustLevel) {
        int count = 0;
        for (int i = 0; i < usage.length(); i++) {
            if (usage.charAt(i) == '<') {
                count++;
            }
        }
        if (usage.contains("<hash>")) count--; // bad fix?
        if ((!inGame && trustLevel > 0)) count--;
        return count;
    }

    private static int getMaximumArgs(String usage, boolean inGame, int trustLevel) {
        int count = 0;
        for (int i = 0; i < usage.length(); i++) {
            if (usage.charAt(i) == '<' || usage.charAt(i) == '[') {
                count++;
            }
        }
        if (!inGame && trustLevel > 0) count++;
        return count;
    }
}
