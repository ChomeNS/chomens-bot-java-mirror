package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.*;
import me.chayapak1.chomens_bot.commands.*;
import me.chayapak1.chomens_bot.util.ExceptionUtilities;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommandHandlerPlugin {
    public static final List<Command> commands = new ArrayList<>();

    static {
        registerCommand(new CommandBlockCommand());
        registerCommand(new CowsayCommand());
        registerCommand(new EchoCommand());
        registerCommand(new HelpCommand());
        registerCommand(new TestCommand());
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
        registerCommand(new EndCommand());
        registerCommand(new CloopCommand());
        registerCommand(new WeatherCommand());
        registerCommand(new TranslateCommand());
        registerCommand(new KickCommand());
        registerCommand(new ClearChatQueueCommand());
        registerCommand(new FilterCommand());
        registerCommand(new MailCommand());
        registerCommand(new EvalCommand());
        registerCommand(new InfoCommand());
        registerCommand(new ConsoleCommand());
//        registerCommand(new ScreenshareCommand());
        registerCommand(new WhitelistCommand());
        registerCommand(new SeenCommand());
        registerCommand(new IPFilterCommand());
        registerCommand(new StopCommand());
        registerCommand(new GrepLogCommand());
        registerCommand(new FindAltsCommand());
        registerCommand(new RestartCommand());
    }

    public boolean disabled = false;

    private final Bot bot;

    private int commandPerSecond = 0;

    public CommandHandlerPlugin (Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(() -> commandPerSecond = 0, 0, 1, TimeUnit.SECONDS);
    }

    public static void registerCommand (Command command) {
        commands.add(command);
    }

    // BETTER QUALITY than the js version
    // BUT still not the best
    public Component executeCommand (
            String input,
            CommandContext context,
            MessageReceivedEvent event
    ) {
        final boolean inGame = context instanceof PlayerCommandContext;
        final boolean discord = context instanceof DiscordCommandContext;
        final boolean console = context instanceof ConsoleCommandContext;

        if (!console) {
            if (disabled) return Component.text("ChomeNS Bot is currently disabled").color(NamedTextColor.RED);
            else if (commandPerSecond > 100) return null;
        }

        commandPerSecond++;

        final String[] splitInput = input.trim().split("\\s+");

        if (splitInput.length == 0) return null;

        final String commandName = splitInput[0];

        final Command command = findCommand(commands, commandName);

        // I think this is kinda annoying when you correct spelling mistakes or something,
        // so I made it return nothing if it's in game
        if (command == null && !inGame) return Component.text("Unknown command: " + commandName).color(NamedTextColor.RED);
        else if (command == null) return null;

        final TrustLevel trustLevel = command.trustLevel;

        if (trustLevel != TrustLevel.PUBLIC && splitInput.length < 2 && inGame) return Component.text("Please provide a hash").color(NamedTextColor.RED);

        String userHash = "";
        if (trustLevel != TrustLevel.PUBLIC && inGame) userHash = splitInput[1];

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);

        final String[] args = Arrays.copyOfRange(splitInput, (trustLevel != TrustLevel.PUBLIC && inGame) ? 2 : 1, splitInput.length);

        if (command.trustLevel != TrustLevel.PUBLIC && !console) {
            if (discord) {
                final Member member = event.getMember();

                if (member == null) return null;

                final List<Role> roles = member.getRoles();

                final boolean hasTrustedRole = roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(bot.config.discord.trustedRoleName));
                final boolean hasAdminRole = roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(bot.config.discord.adminRoleName));
                final boolean hasOwnerRole = roles.stream().anyMatch(role -> role.getName().equalsIgnoreCase(bot.config.discord.ownerRoleName));

                if (
                        command.trustLevel == TrustLevel.TRUSTED &&
                                !hasTrustedRole &&
                                !hasAdminRole &&
                                !hasOwnerRole
                ) return Component.text("You're not in the trusted role!").color(NamedTextColor.RED);

                if (
                        command.trustLevel == TrustLevel.ADMIN &&
                                !hasAdminRole &&
                                !hasOwnerRole
                ) return Component.text("You're not in the admin role!").color(NamedTextColor.RED);

                if (
                        command.trustLevel == TrustLevel.OWNER &&
                                !hasOwnerRole
                ) return Component.text("You're not in the owner role!").color(NamedTextColor.RED);

                context.trustLevel = hasOwnerRole ? TrustLevel.OWNER :
                                hasAdminRole ? TrustLevel.ADMIN :
                                hasTrustedRole ? TrustLevel.TRUSTED :
                                TrustLevel.PUBLIC;
            } else {
                final boolean correctHash = bot.hashing.isCorrectHash(userHash, splitInput[0], context.sender);
                final boolean correctAdminHash = bot.hashing.isCorrectAdminHash(userHash, splitInput[0], context.sender);
                final boolean correctOwnerHash = bot.hashing.isCorrectOwnerHash(userHash, splitInput[0], context.sender);

                if (
                        command.trustLevel == TrustLevel.TRUSTED &&
                                !correctHash &&
                                !correctAdminHash &&
                                !correctOwnerHash
                ) return Component.text("Invalid hash").color(NamedTextColor.RED);

                if (
                        command.trustLevel == TrustLevel.ADMIN &&
                                !correctAdminHash &&
                                !correctOwnerHash
                ) return Component.text("Invalid admin hash").color(NamedTextColor.RED);

                if (
                        command.trustLevel == TrustLevel.OWNER &&
                                !correctOwnerHash
                ) return Component.text("Invalid owner hash").color(NamedTextColor.RED);

                context.trustLevel = correctOwnerHash ? TrustLevel.OWNER :
                                correctAdminHash ? TrustLevel.ADMIN :
                                correctHash ? TrustLevel.TRUSTED :
                                TrustLevel.PUBLIC;
            }
        }

        if (!console && command.consoleOnly) return Component.text("This command can only be ran via console").color(NamedTextColor.RED);

        // should these be here?
        context.fullArgs = fullArgs;
        context.args = args;
        context.commandName = command.name;
        context.userInputCommandName = commandName;

        try {
            return command.execute(context);
        } catch (CommandException e) {
            return e.message.color(NamedTextColor.RED);
        } catch (Exception e) {
            e.printStackTrace();

            final String stackTrace = ExceptionUtilities.getStacktrace(e);
            if (inGame) {
                if (bot.options.useChat || !bot.options.useCore) return Component.text(e.toString()).color(NamedTextColor.RED);
                return Component
                        .text("An error occurred while trying to execute the command, hover here for stacktrace", NamedTextColor.RED)
                        .hoverEvent(
                                HoverEvent.showText(
                                        Component
                                                .text(stackTrace)
                                                .color(NamedTextColor.RED)
                                )
                        )
                        .color(NamedTextColor.RED);
            } else {
                return Component.text(stackTrace).color(NamedTextColor.RED);
            }
        }
    }

    public Command findCommand (List<Command> commands, String searchTerm) {
        for (Command command : commands) {
            if (
                    (
                            command.name.equals(searchTerm.toLowerCase()) ||
                                    Arrays.stream(command.aliases).toList().contains(searchTerm.toLowerCase())
                    ) &&
                            !searchTerm.isEmpty() // ig yup
            ) {
                return command;
            }
        }
        return null;
    }
}
