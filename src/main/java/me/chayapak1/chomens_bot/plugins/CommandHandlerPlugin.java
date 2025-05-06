package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.*;
import me.chayapak1.chomens_bot.commands.*;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.ExceptionUtilities;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
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
            new NetCommandCommand()
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

        final TrustLevel trustLevel = command.trustLevel;

        if (trustLevel != TrustLevel.PUBLIC && splitInput.length < 2 && inGame) {
            context.sendOutput(Component.translatable("command_handler.no_hash_provided", NamedTextColor.RED));
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
                            Component.translatable("command_handler.not_enough_roles", NamedTextColor.RED)
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
                                            "command_handler.not_enough_roles.trust_level",
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
            final TrustLevel[] values = TrustLevel.values();
            context.trustLevel = values[values.length - 1]; // highest level
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

    public Component renderTranslatable (final Component original) {
        final List<Component> renderedChildren = new ArrayList<>();
        final List<TranslationArgument> renderedArguments = new ArrayList<>();

        for (final Component child : original.children()) {
            final Component rendered = renderTranslatable(child);
            renderedChildren.add(rendered);
        }

        if (original instanceof final TranslatableComponent translatableComponent) {
            for (final TranslationArgument argument : translatableComponent.arguments()) {
                final Component rendered = renderTranslatable(argument.asComponent());
                renderedArguments.add(TranslationArgument.component(rendered));
            }
        }

        final HoverEvent<?> hoverEvent = original.hoverEvent();
        final HoverEventSource<Component> hoverEventSource;
        if (
                hoverEvent != null
                    && hoverEvent.action() == HoverEvent.Action.SHOW_TEXT
                    && hoverEvent.value() instanceof final Component hoverComponent
        ) {
            final Component rendered = renderTranslatable(hoverComponent);
            hoverEventSource = HoverEvent.showText(rendered);
        } else {
            hoverEventSource = null;
        }

        if (original instanceof final TranslatableComponent translatableComponent) {
            final String translated = I18nUtilities.get(translatableComponent.key());

            return translatableComponent
                    .key(
                            (translated == null ? "" : "chomens_bot.") + // sometimes we can collide with minecraft's
                                    translatableComponent.key()
                    )
                    .fallback(translated)
                    .children(renderedChildren)
                    .arguments(renderedArguments)
                    .hoverEvent(hoverEventSource);
        } else {
            return original
                    .children(renderedChildren)
                    .hoverEvent(hoverEventSource);
        }
    }
}
