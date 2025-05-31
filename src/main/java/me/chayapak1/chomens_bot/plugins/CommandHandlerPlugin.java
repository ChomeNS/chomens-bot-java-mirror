package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.*;
import me.chayapak1.chomens_bot.commands.*;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.ExceptionUtilities;
import me.chayapak1.chomens_bot.util.HashingUtilities;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CommandHandlerPlugin implements Listener {
    public static final List<Command> COMMANDS = ObjectList.of(
            new CommandBlockCommand(),
            new CowsayCommand(),
            new EchoCommand(),
            new HelpCommand(),
            new TestCommand(),
            new ValidateCommand(),
            new MusicCommand(),
            new RandomTeleportCommand(),
            new BotVisibilityCommand(),
            new TPSBarCommand(),
            new NetMessageCommand(),
            new RefillCoreCommand(),
            new WikipediaCommand(),
            new UrbanCommand(),
            new ClearChatCommand(),
            new ListCommand(),
            new ServerEvalCommand(),
            new UUIDCommand(),
            new TimeCommand(),
            new BruhifyCommand(),
            new EndCommand(),
            new CloopCommand(),
            new WeatherCommand(),
            new TranslateCommand(),
            new KickCommand(),
            new ClearChatQueueCommand(),
            new FilterCommand(),
            new MailCommand(),
            new EvalCommand(),
            new InfoCommand(),
            new ConsoleCommand(),
            // new ScreenshareCommand(),
            new WhitelistCommand(),
            new SeenCommand(),
            new IPFilterCommand(),
            new StopCommand(),
            new GrepLogCommand(),
            new FindAltsCommand(),
            new RestartCommand(),
            new NetCommandCommand(),
            new AuthCommand()
    );

    public static Command findCommand (final String searchTerm) {
        if (searchTerm.isBlank()) return null;

        for (final Command command : COMMANDS) {
            if (
                    command.name.equals(searchTerm.toLowerCase()) ||
                            Arrays.asList(command.aliases).contains(searchTerm.toLowerCase())
            ) {
                return command;
            }
        }

        return null;
    }

    public boolean disabled = false;

    private final Bot bot;

    private final AtomicInteger commandsPerSecond = new AtomicInteger();

    public CommandHandlerPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onLocalSecondTick () {
        commandsPerSecond.set(0);
    }

    // BETTER QUALITY than the js version
    // BUT still not the best
    // and also not really optimized
    // (sometimes execution time can be as high as 19 ms,
    // though it can also be as low as 4000 ns)
    public void executeCommand (final String input, final CommandContext context) {
        if (commandsPerSecond.get() > 100) return;
        else commandsPerSecond.getAndIncrement();

        final boolean inGame = context instanceof PlayerCommandContext;
        final boolean bypass = isBypassContext(context);

        final String[] splitInput = input.trim().split("\\s+");
        if (splitInput.length == 0) return;

        final String commandName = splitInput[0];
        final Command command = findCommand(commandName);
        if (command == null) {
            if (!inGame)
                context.sendOutput(
                        Component.translatable(
                                "command_handler.unknown_command",
                                NamedTextColor.RED,
                                Component.text(commandName)
                        )
                );
            return;
        }

        if (!bypass && !isCommandAllowed(context, command)) return;

        final TrustLevel authenticatedTrustLevel = context.sender.authenticatedTrustLevel;
        final boolean authenticated = isAuthenticated(context, authenticatedTrustLevel);
        final boolean needsHash = needsHash(command, inGame, authenticated, splitInput.length);

        final String userHash = needsHash ? splitInput[1] : "";
        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);
        final String[] args = needsHash ? Arrays.copyOfRange(splitInput, 2, splitInput.length) : fullArgs;

        if (!checkTrustLevel(context, command, userHash, authenticated, bypass))
            return;

        if (!bypass && command.consoleOnly) {
            context.sendOutput(Component.translatable("command_handler.console_only", NamedTextColor.RED));
            return;
        }

        context.fullArgs = fullArgs;
        context.args = args;
        context.commandName = command.name;
        context.userInputCommandName = commandName;

        handleExecution(command, context, inGame);
    }

    private boolean isBypassContext (final CommandContext context) {
        return context instanceof ConsoleCommandContext || context instanceof ChomeNSModCommandContext;
    }

    private boolean isAuthenticated (final CommandContext context, final TrustLevel authenticatedTrustLevel) {
        return context instanceof final PlayerCommandContext playerContext
                && playerContext.packetType == ChatPacketType.PLAYER
                && authenticatedTrustLevel != TrustLevel.PUBLIC;
    }

    private boolean needsHash (final Command command, final boolean inGame, final boolean authenticated, final int inputLength) {
        return command.trustLevel != TrustLevel.PUBLIC && inGame && !authenticated && inputLength < 2;
    }

    private boolean checkTrustLevel (
            final CommandContext context,
            final Command command,
            final String userHash,
            final boolean authenticated,
            final boolean bypass
    ) {
        final TrustLevel requiredLevel = command.trustLevel;

        if (requiredLevel == TrustLevel.PUBLIC || bypass) {
            context.trustLevel = bypass ? TrustLevel.MAX : TrustLevel.PUBLIC;
            return true;
        }

        if (context instanceof final RemoteCommandContext remote) {
            if (remote.source.trustLevel.level < requiredLevel.level) {
                context.sendOutput(Component.translatable("command_handler.not_enough_roles", NamedTextColor.RED));
                return false;
            }
            context.trustLevel = remote.source.trustLevel;
            return true;
        }

        if (context instanceof final DiscordCommandContext discord) {
            final Member member = discord.member;
            if (member == null) return false;

            final TrustLevel userLevel = TrustLevel.fromDiscordRoles(member.getRoles());
            if (userLevel.level < requiredLevel.level) {
                context.sendOutput(
                        Component.translatable(
                                "command_handler.not_enough_roles.trust_level",
                                NamedTextColor.RED,
                                Component.text(requiredLevel.name())
                        )
                );
                return false;
            }
            context.trustLevel = userLevel;
            return true;
        }

        if (authenticated) {
            final TrustLevel authLevel = context.sender.authenticatedTrustLevel;
            if (authLevel.level < requiredLevel.level) {
                context.sendOutput(Component.translatable("command_handler.not_enough_roles", NamedTextColor.RED));
                return false;
            }
            context.trustLevel = authLevel;
            return true;
        }

        final TrustLevel hashLevel = HashingUtilities.getTrustLevel(userHash, command.name, context.sender);
        if (hashLevel.level < requiredLevel.level) {
            context.sendOutput(
                    Component.translatable(
                            "command_handler.invalid_hash",
                            NamedTextColor.RED,
                            Component.text(requiredLevel.toString())
                    )
            );
            return false;
        }

        context.trustLevel = hashLevel;
        return true;
    }

    private boolean isCommandAllowed (final CommandContext context, final Command command) {
        if (disabled) {
            context.sendOutput(Component.translatable("command_handler.disabled", NamedTextColor.RED));
            return false;
        }

        return !(context instanceof final PlayerCommandContext playerContext) ||
                command.disallowedPacketTypes == null ||
                !Arrays.asList(command.disallowedPacketTypes).contains(playerContext.packetType);
    }

    private void handleExecution (final Command command, final CommandContext context, final boolean inGame) {
        try {
            final Component output = command.execute(context);
            if (output != null) context.sendOutput(output);
        } catch (final CommandException e) {
            context.sendOutput(e.message.colorIfAbsent(NamedTextColor.RED));
        } catch (final Exception e) {
            bot.logger.error(e);
            final String stackTrace = ExceptionUtilities.getStacktrace(e);
            if (inGame && (bot.options.useChat || !bot.options.useCore)) {
                context.sendOutput(Component.text(e.toString(), NamedTextColor.RED));
            } else {
                context.sendOutput(
                        Component
                                .translatable("command_handler.exception", NamedTextColor.RED)
                                .hoverEvent(HoverEvent.showText(Component.text(stackTrace, NamedTextColor.RED)))
                );
            }
        }
    }
}
