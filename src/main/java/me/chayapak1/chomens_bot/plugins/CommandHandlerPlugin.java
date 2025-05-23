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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
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
                            Arrays.stream(command.aliases).toList().contains(searchTerm.toLowerCase())
            ) {
                return command;
            }
        }

        return null;
    }

    public boolean disabled = false;

    private final Bot bot;

    private static final AtomicInteger commandsPerSecond = new AtomicInteger();

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
    public void executeCommand (
            final String input,
            final CommandContext context
    ) {
        final boolean inGame = context instanceof PlayerCommandContext;

        final boolean bypass = context instanceof ConsoleCommandContext || context instanceof ChomeNSModCommandContext;

        if (commandsPerSecond.get() > 100) return;
        else commandsPerSecond.getAndIncrement();

        final String[] splitInput = input.trim().split("\\s+");

        if (splitInput.length == 0) return;

        final String commandName = splitInput[0];

        final Command command = findCommand(commandName);

        // I think this is kinda annoying when you correct spelling mistakes or something,
        // so I made it return nothing if we're in game
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

        if (!bypass) {
            if (disabled) {
                context.sendOutput(Component.translatable("command_handler.disabled", NamedTextColor.RED));
                return;
            } else if (
                    context instanceof final PlayerCommandContext playerContext &&
                            command.disallowedPacketTypes != null &&
                            Arrays.asList(command.disallowedPacketTypes).contains(playerContext.packetType)
            ) {
                return;
            }
        }

        final TrustLevel authenticatedTrustLevel = context.sender.authenticatedTrustLevel;

        final boolean authenticated = context instanceof final PlayerCommandContext playerContext
                && playerContext.packetType == ChatPacketType.PLAYER
                && authenticatedTrustLevel != TrustLevel.PUBLIC;

        final TrustLevel trustLevel = command.trustLevel;

        if (trustLevel != TrustLevel.PUBLIC && splitInput.length < 2 && inGame && !authenticated) {
            context.sendOutput(Component.translatable("command_handler.no_hash_provided", NamedTextColor.RED));
            return;
        }

        final boolean needsHash = trustLevel != TrustLevel.PUBLIC && inGame && !authenticated;

        final String userHash = needsHash ? splitInput[1] : "";

        final String[] fullArgs = Arrays.copyOfRange(splitInput, 1, splitInput.length);
        final String[] args = needsHash
                ? Arrays.copyOfRange(splitInput, 2, splitInput.length)
                : fullArgs;

        // LoL ohio spaghetti code
        if (command.trustLevel != TrustLevel.PUBLIC && !bypass) {
            if (context instanceof final RemoteCommandContext remote) {
                if (remote.source.trustLevel.level < trustLevel.level) {
                    context.sendOutput(
                            Component.translatable("command_handler.not_enough_roles", NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = remote.source.trustLevel;
            } else if (context instanceof final DiscordCommandContext discordCommandContext) {
                final Member member = discordCommandContext.member;

                if (member == null) return;

                final List<Role> roles = member.getRoles();

                final TrustLevel userTrustLevel = TrustLevel.fromDiscordRoles(roles);

                if (trustLevel.level > userTrustLevel.level) {
                    context.sendOutput(
                            Component
                                    .translatable(
                                            "command_handler.not_enough_roles.trust_level",
                                            Component.text(trustLevel.name())
                                    )
                                    .color(NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = userTrustLevel;
            } else if (authenticated) {
                if (trustLevel.level > authenticatedTrustLevel.level) {
                    context.sendOutput(
                            Component.translatable("command_handler.not_enough_roles", NamedTextColor.RED)
                    );
                    return;
                }

                context.trustLevel = authenticatedTrustLevel;
            } else {
                final TrustLevel userTrustLevel = bot.hashing.getTrustLevel(userHash, splitInput[0], context.sender);

                if (trustLevel.level > userTrustLevel.level) {
                    context.sendOutput(
                            Component
                                    .translatable(
                                            "command_handler.invalid_hash",
                                            NamedTextColor.RED,
                                            Component.text(trustLevel.toString())
                                    )
                    );
                    return;
                }

                context.trustLevel = userTrustLevel;
            }
        } else if (bypass) {
            context.trustLevel = TrustLevel.MAX;
        }

        // should i give access to all bypass contexts instead of only console?
        if (!bypass && command.consoleOnly) {
            context.sendOutput(Component.translatable("command_handler.console_only", NamedTextColor.RED));
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
                                    .translatable("command_handler.exception", NamedTextColor.RED)
                                    .hoverEvent(
                                            HoverEvent.showText(
                                                    Component.text(stackTrace, NamedTextColor.RED)
                                            )
                                    )
                    );
            } else {
                context.sendOutput(Component.text(stackTrace, NamedTextColor.RED));
            }
        }
    }
}
