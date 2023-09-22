package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.ConsoleCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.dv8tion.jda.api.JDA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jline.reader.*;

import java.util.ArrayList;
import java.util.List;

public class ConsolePlugin implements Completer {
    private final List<Bot> allBots;

    public final LineReader reader;

    public String consoleServer = "all";

    public String prefix;

    public Component formatPrefix;

    private static final List<Listener> listeners = new ArrayList<>();

    public ConsolePlugin (List<Bot> allBots, Configuration discordConfig, JDA jda) {
        this.allBots = allBots;
        this.reader = LineReaderBuilder
                .builder()
                .completer(this)
                .build();

        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        for (Bot bot : allBots) {
            prefix = bot.config.consoleCommandPrefix;

            bot.console = this;

            bot.addListener(new Bot.Listener() {
                @Override
                public void connected(ConnectedEvent event) {
                    bot.console.formatPrefix = Component.text(bot.username + " Console").color(NamedTextColor.GRAY);
                }
            });

            bot.logger = new LoggerPlugin(bot);
        }

        new DiscordPlugin(discordConfig, jda);

        final String prompt = "> ";

        Main.executorService.submit(() -> {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt);
                } catch (Exception e) {
                    System.exit(1);
                }

                handleLine(line);
            }
        });

        for (Listener listener : listeners) { listener.ready(); }
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (!line.line().startsWith(".")) return;

        final String command = line.line().substring(prefix.length());

        final List<Command> commands = CommandHandlerPlugin.commands;

        final List<String> commandNames = commands.stream().map((eachCommand) -> eachCommand.name).toList();

        final List<Candidate> filteredCommands = commandNames
                .stream()
                .filter((eachCommand) -> eachCommand.startsWith(command))
                .map((eachCommand) -> new Candidate(prefix + eachCommand))
                .toList();

        candidates.addAll(filteredCommands);
    }

    public void handleLine (String line) {
        if (line == null) return;

        for (Bot bot : allBots) {
            final String hostAndPort = bot.host + ":" + bot.port;

            if (!hostAndPort.equals(consoleServer) && !consoleServer.equals("all")) continue;

            if (line.startsWith(prefix)) {
                final ConsoleCommandContext context = new ConsoleCommandContext(bot, prefix);

                final Component output = bot.commandHandler.executeCommand(line.substring(prefix.length()), context, null);

                if (output != null) {
                    context.sendOutput(output);
                }

                continue;
            }

            bot.chat.tellraw(
                    Component.translatable(
                            "[%s] %s › %s",
                            formatPrefix,
                            Component.text(bot.config.ownerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.ownerName)),
                            Component.text(line).color(NamedTextColor.GRAY)
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }
    }

    public static void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void ready () {}
    }
}
