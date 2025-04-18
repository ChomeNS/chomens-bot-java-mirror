package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.*;
import me.chayapak1.chomens_bot.commands.*;
import me.chayapak1.chomens_bot.data.listener.Listener;
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

public class CommandHandlerPlugin implements Listener {
    public static final List<Command> COMMANDS = new ArrayList<>();

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
        registerCommand(new NetCommandCommand());
    }

    public static void registerCommand (final Command command) {
        COMMANDS.add(command);
    }

    public static Command findCommand (final String searchTerm) {
        if (searchTerm.isBlank()) return null;

        for (final Command command : COMMANDS) {
            if (
                    command.name.equals(searchTerm.toLowerCase()) ||
                            Arrays.stream(command.aliases).toList().contains(searchTerm.toLowerCase())
            ) {
                return command;
            }
        }

        return null;
    }

    public boolean disabled = false;

    private final Bot bot;

    private int commandPerSecond = 0;

    public CommandHandlerPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onSecondTick () {
        commandPerSecond = 0;
    }

    // BETTER QUALITY than the js version
    // BUT still not the best
    // and also not really optimized
    // (sometimes execution time can be as high as 19 ms,
    // though it can also be as low as 4000 ns)
    public void executeCommand (
            final String input,
            final CommandContext context
    ) {
        final boolean inGame = context instanceof PlayerCommandContext;

        final boolean discord;
        final MessageReceivedEvent event;

        if (context instanceof final DiscordCommandContext discordContext) {
            discord = true;
            event = discordContext.event;
        } else {
            discord = false;
            event = null;
        }

        final boolean bypass = context instanceof ConsoleCommandContext || context instanceof ChomeNSModCommandContext;

        if (commandPerSecond > 100) return;

        commandPerSecond++;

        final String[] splitInput = input.trim().split("\\s+");

        if (splitInput.length == 0) return;

        final String commandName = splitInput[0];

        final Command command = findCommand(commandName);

        // I think this is kinda annoying when you correct spelling mistakes or something,
        // so I made it return nothing if we're in game
        if (command == null) {
            if (!inGame)
                context.sendOutput(Component.text("Unknown command: " + commandName).color(NamedTextColor.RED));

            return;
        }

        if (!bypass) {
            if (disabled) {
                context.sendOutput(Component.text("ChomeNS Bot is currently disabled").color(NamedTextColor.RED));
                return;
            } else if (
                    context instanceof final PlayerCommandContext playerContext &&
                            command.disallowedPacketTypes != null &&
                            Arrays.asList(command.disallowedPacketTypes).contains(playerContext.packetType)
            ) {
                return;
            }
        }

        final TrustLevel trustLevel = command.trustLevel;

        if (trustLevel != TrustLevel.PUBLIC && splitInput.length < 2 && inGame) {
            context.sendOutput(Component.text("Please provide a hash").color(NamedTextColor.RED));
            return;
        }

        String userHash = "";
        if (trustLevel != TrustLevel.PUBLIC && inGame) userHash = splitInput[1];

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);

        final String[] args = Arrays.copyOfRange(splitInput, (trustLevel != TrustLevel.PUBLIC && inGame) ? 2 : 1, splitInput.length);

        // LoL ohio spaghetti code
        if (command.trustLevel != TrustLevel.PUBLIC && !bypass) {
            if (context instanceof final RemoteCommandContext remote) {
                if (remote.source.trustLevel.level < trustLevel.level) {
                    context.sendOutput(
                            Component
                                    .text("Your don't have enough permission to execute this command!")
                                    .color(NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = remote.source.trustLevel;
            } else if (discord) {
                final Member member = event.getMember();

                if (member == null) return;

                final List<Role> roles = member.getRoles();

                final TrustLevel userTrustLevel = TrustLevel.fromDiscordRoles(roles);

                if (trustLevel.level > userTrustLevel.level) {
                    context.sendOutput(
                            Component
                                    .translatable(
                                            "Your current roles don't allow you to execute %s commands!",
                                            Component.text(trustLevel.name())
                                    )
                                    .color(NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = userTrustLevel;
            } else {
                final TrustLevel userTrustLevel = bot.hashing.getTrustLevel(userHash, splitInput[0], context.sender);

                if (trustLevel.level > userTrustLevel.level) {
                    context.sendOutput(
                            Component
                                    .translatable(
                                            "Invalid %s hash",
                                            Component.text(trustLevel.name())
                                    )
                                    .color(NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = userTrustLevel;
            }
        } else if (bypass) {
            final TrustLevel[] values = TrustLevel.values();
            context.trustLevel = values[values.length - 1]; // highest level
        }

        // should i give access to all bypass contexts instead of only console?
        if (!bypass && command.consoleOnly) {
            context.sendOutput(Component.text("This command can only be run via console").color(NamedTextColor.RED));
            return;
        }

        // should these be here?
        context.fullArgs = fullArgs;
        context.args = args;
        context.commandName = command.name;
        context.userInputCommandName = commandName;

        try {
            final Component output = command.execute(context);

            if (output != null) context.sendOutput(output);
        } catch (final CommandException e) {
            context.sendOutput(e.message.color(NamedTextColor.RED));
        } catch (final Exception e) {
            bot.logger.error(e);

            final String stackTrace = ExceptionUtilities.getStacktrace(e);
            if (inGame) {
                if (bot.options.useChat || !bot.options.useCore)
                    context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
                else
                    context.sendOutput(
                            Component
                                    .text("An error occurred while trying to execute the command, hover here for stacktrace", NamedTextColor.RED)
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component
                                                            .text(stackTrace)
                                                            .color(NamedTextColor.RED)
                                            )
                                    )
                                    .color(NamedTextColor.RED)
                    );
            } else {
                context.sendOutput(Component.text(stackTrace).color(NamedTextColor.RED));
            }
        }
    }
}
