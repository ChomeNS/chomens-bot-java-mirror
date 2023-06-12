package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.commands.*;
import land.chipmunk.chayapak.chomens_bot.util.ExceptionUtilities;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandlerPlugin {
    private final Bot bot;

    @Getter private final List<Command> commands = new ArrayList<>();

    @Getter @Setter private boolean disabled = false;

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
        registerCommand(new UptimeCommand());
        registerCommand(new MailCommand());
    }

    public void registerCommand (Command command) {
        commands.add(command);
    }

    // literally the same quality as the js chomens bot
    // well probably less mess (mabe.,,.)
    public Component executeCommand (
            String input,
            CommandContext context,
            boolean inGame,
            boolean discord,
            boolean console,
            MessageReceivedEvent event
    ) {
        if (disabled) return null;

        final String[] splitInput = input.trim().split("\\s+");

        final String commandName = splitInput[0];

        final Command command = findCommand(commands, commandName);

        // I think this is kinda annoying when you correct spelling mistakes or something,
        // so I made it return nothing if it's in game
        if (command == null && !inGame) return Component.text("Unknown command: " + commandName).color(NamedTextColor.RED);
        else if (command == null) return null;

        final TrustLevel trustLevel = command.trustLevel();

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);

        // TODO: improve these minimum args and maximum args stuff, the current one really sucks.,.,
        final int shortestUsageIndex = getShortestUsageIndex(command.usage());
        final int longestUsageIndex = getLongestUsageIndex(command.usage());
        final String shortestUsage = command.usage().get(shortestUsageIndex);
        final String longestUsage = command.usage().get(longestUsageIndex);

        final int minimumArgs = getMinimumArgs(shortestUsage, inGame, command.trustLevel());
        final int maximumArgs = getMaximumArgs(longestUsage, inGame, command.trustLevel());
        if (fullArgs.length < minimumArgs) return Component.text("Excepted minimum of " + minimumArgs + " argument(s), got " + fullArgs.length).color(NamedTextColor.RED);
        if (fullArgs.length > maximumArgs && !longestUsage.contains("{")) return Component.text("Too much arguments, expected " + maximumArgs + " max").color(NamedTextColor.RED);

        if (trustLevel != TrustLevel.PUBLIC && splitInput.length < 2 && inGame) return Component.text("Please provide a hash").color(NamedTextColor.RED);

        String userHash = "";
        if (trustLevel != TrustLevel.PUBLIC && inGame) userHash = splitInput[1];

        final String[] args = Arrays.copyOfRange(splitInput, (trustLevel != TrustLevel.PUBLIC && inGame) ? 2 : 1, splitInput.length);

        if (command.trustLevel() != TrustLevel.PUBLIC && !console) {
            if (discord) {
                final List<Role> roles = event.getMember().getRoles();

                final String trustedRoleName = bot.config().discord().trustedRoleName();
                final String adminRoleName = bot.config().discord().adminRoleName();

                if (
                        command.trustLevel() == TrustLevel.TRUSTED &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(trustedRoleName)) &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(adminRoleName))
                ) return Component.text("You're not in the trusted role!").color(NamedTextColor.RED);

                if (
                        command.trustLevel() == TrustLevel.ADMIN &&
                                roles.stream().noneMatch(role -> role.getName().equalsIgnoreCase(adminRoleName))
                ) return Component.text("You're not in the admin role!").color(NamedTextColor.RED);
            } else {
                if (
                        command.trustLevel() == TrustLevel.TRUSTED &&
                                !userHash.equals(bot.hashing().hash()) &&
                                !userHash.equals(bot.hashing().ownerHash())
                ) return Component.text("Invalid hash").color(NamedTextColor.RED);

                if (
                        command.trustLevel() == TrustLevel.ADMIN &&
                                !userHash.equals(bot.hashing().ownerHash())
                ) return Component.text("Invalid OwnerHash").color(NamedTextColor.RED);
            }
        }

        try {
            return command.execute(context, args, fullArgs);
        } catch (Exception e) {
            e.printStackTrace();

            final String stackTrace = ExceptionUtilities.getStacktrace(e);
            if (inGame) {
                if (bot.options().useChat() || !bot.options().useCore()) return Component.text(e.toString()).color(NamedTextColor.RED);
                return Component
                        .text("An error occurred while trying to execute the command, hover here for stacktrace", NamedTextColor.RED)
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

    public Command findCommand (List<Command> commands, String searchTerm) {
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

    private int getLongestUsageIndex(List<String> usages) {
        int longestIndex = 0;
        int maxLength = 0;

        final int usagesSize = usages.size();

        for (int i = 0; i < usagesSize; i++) {
            String[] args = usages.get(i).split("\\s+");
            if (args.length > maxLength) {
                longestIndex = i;
                maxLength = args.length;
            }
        }
        return longestIndex;
    }

    private int getShortestUsageIndex(List<String> usages) {
        int shortestIndex = 0;
        int minLength = Integer.MAX_VALUE;

        final int usagesSize = usages.size();

        for (int i = 0; i < usagesSize; i++) {
            String[] args = usages.get(i).split("\\s+");
            if (args.length < minLength) {
                shortestIndex = i;
                minLength = args.length;
            }
        }
        return shortestIndex;
    }

    private int getMinimumArgs(String usage, boolean inGame, TrustLevel trustLevel) {
        int count = 0;

        final int usageLength = usage.length();

        for (int i = 0; i < usageLength; i++) {
            if (usage.charAt(i) == '<') {
                count++;
            }
        }
        if (usage.contains("<hash>")) count--; // bad fix?
        if ((!inGame && trustLevel != TrustLevel.PUBLIC)) count--;
        return count;
    }

    private int getMaximumArgs(String usage, boolean inGame, TrustLevel trustLevel) {
        int count = 0;

        final int usageLength = usage.length();

        for (int i = 0; i < usageLength; i++) {
            if (usage.charAt(i) == '<' || usage.charAt(i) == '[') {
                count++;
            }
        }
        if (!inGame && trustLevel != TrustLevel.PUBLIC) count++;
        return count;
    }
}
