package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.contexts.ConsoleCommandContext;
import me.chayapak1.chomens_bot.util.ChatMessageUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import org.jline.reader.*;

import java.util.List;
import java.util.Map;

public class ConsolePlugin implements Completer {
    private static final ConsoleFormatRenderer RENDERER = new ConsoleFormatRenderer();

    private final List<Bot> allBots;

    public final LineReader reader;

    public String consoleServer = "all";

    private final String prefix;

    private final Component format;

    public ConsolePlugin (final Configuration config) {
        this.allBots = Main.bots;
        this.format = GsonComponentSerializer.gson().deserialize(config.consoleChatFormat);
        this.prefix = config.consoleCommandPrefix;
        this.reader = LineReaderBuilder
                .builder()
                .completer(this)
                .build();

        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        final Thread thread = new Thread(
                () -> {
                    while (true) {
                        String line = null;
                        try {
                            line = reader.readLine(getPrompt());
                        } catch (final Exception e) {
                            System.exit(1);
                        }

                        handleLine(line);
                    }
                },
                "Console Thread"
        );

        thread.start();
    }

    private String getPrompt () {
        // [all] > input
        // [localhost:25565] > input
        return String.format(
                "[%s] > ",
                consoleServer
        );
    }

    @Override
    public void complete (final LineReader reader, final ParsedLine line, final List<Candidate> candidates) {
        if (!line.line().startsWith(prefix)) return;

        final String command = line.line().substring(prefix.length());

        final List<Command> commands = CommandHandlerPlugin.COMMANDS;

        final List<String> commandNames = commands.stream().map((eachCommand) -> eachCommand.name).toList();

        final List<Candidate> filteredCommands = commandNames
                .stream()
                .filter((eachCommand) -> eachCommand.startsWith(command))
                .map((eachCommand) -> new Candidate(prefix + eachCommand))
                .toList();

        candidates.addAll(filteredCommands);
    }

    private void handleLine (final String line) {
        if (line == null) return;

        for (final Bot bot : allBots) {
            final String server = bot.getServerString(true);

            if (!server.equals(consoleServer) && !consoleServer.equalsIgnoreCase("all")) continue;

            if (line.startsWith(prefix)) {
                final ConsoleCommandContext context = new ConsoleCommandContext(bot, prefix);
                bot.commandHandler.executeCommand(line.substring(prefix.length()), context);
                continue;
            }

            if (!bot.loggedIn) continue;

            final Component stylizedMessage = ChatMessageUtilities.applyChatMessageStyling(line);

            final Component rendered = RENDERER.render(
                    format,
                    new ConsoleFormatContext(
                            bot.profile.getIdAsString(),
                            stylizedMessage,
                            Map.of(
                                    "MESSAGE", line,
                                    "USERNAME", bot.profile.getName(),
                                    "OWNER_NAME", bot.config.ownerName
                            )
                    )
            );

            bot.chat.tellraw(rendered);
        }
    }

    // Everything below is from https://code.chipmunk.land/ChomeNS/chipmunkmod/src/branch/1.21.4/src/main/java/land/chipmunk/chipmunkmod/modules/custom_chat
    // Thank you, Amy!
    private record ConsoleFormatContext(String uuid, Component message, Map<String, String> args) { }

    private final static class ConsoleFormatRenderer extends TranslatableComponentRenderer<ConsoleFormatContext> {
        @Override
        protected @NotNull Component renderSelector (final @NotNull SelectorComponent component,
                                                     final @NotNull ConsoleFormatContext context) {
            final String pattern = component.pattern();
            if (pattern.equals("@s")) {
                final SelectorComponent.Builder builder = Component.selector()
                        .pattern(context.uuid());

                return this.mergeStyleAndOptionallyDeepRender(component, builder, context);
            }

            return super.renderSelector(component, context);
        }

        @Override
        protected @NotNull Component renderText (final @NotNull TextComponent component,
                                                 final @NotNull ConsoleFormatContext context) {
            final String content = component.content();
            if (content.equals("MESSAGE")) {
                return this.mergeMessage(component, context.message(), context);
            }

            final String arg = context.args().get(component.content());
            if (arg != null) {
                final TextComponent.Builder builder = Component.text()
                        .content(arg);

                return this.mergeStyleAndOptionallyDeepRender(component, builder, context);
            }

            return super.renderText(component, context);
        }

        @SuppressWarnings("NonExtendableApiUsage") // we're not extending it silly
        @Override
        protected <B extends ComponentBuilder<?, ?>> void mergeStyle (final Component component, final B builder,
                                                                      final ConsoleFormatContext context) {
            super.mergeStyle(component, builder, context);

            // render clickEvent that may contain something like "MESSAGE"
            // HoverEvent already handled by super
            builder.clickEvent(this.mergeClickEvent(component.clickEvent(), context));
        }

        private Component mergeMessage (final Component root, final Component msg, final ConsoleFormatContext context) {
            Component result = msg.applyFallbackStyle(root.style()); // applyFallbackStyle will apply everything that isn't content

            final ClickEvent clickEvent = result.clickEvent();
            if (clickEvent != null) {
                result = result.clickEvent(mergeClickEvent(clickEvent, context));
            }

            final HoverEvent<?> hoverEvent = result.hoverEvent();
            if (hoverEvent != null) {
                result = result.hoverEvent(hoverEvent.withRenderedValue(this, context));
            }

            return result;
        }

        private ClickEvent mergeClickEvent (final ClickEvent clickEvent, final ConsoleFormatContext context) {
            if (clickEvent == null) return null;

            final String value = clickEvent.value();
            final String arg = context.args().get(value);
            if (arg == null) return clickEvent;

            return ClickEvent.clickEvent(clickEvent.action(), arg);
        }
    }
}
