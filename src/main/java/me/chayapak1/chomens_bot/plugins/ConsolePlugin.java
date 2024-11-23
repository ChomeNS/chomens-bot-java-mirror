package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.ConsoleCommandContext;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jline.reader.*;

import java.util.List;

public class ConsolePlugin implements Completer {
    private final List<Bot> allBots;

    public final LineReader reader;

    public String consoleServer = "all";

    private String prefix;

    public ConsolePlugin () {
        this.allBots = Main.bots;
        this.reader = LineReaderBuilder
                .builder()
                .completer(this)
                .build();

        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        for (Bot bot : allBots) {
            prefix = bot.config.consoleCommandPrefix;

            bot.console = this;
        }

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

    private void handleLine (String line) {
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
                            Component.text(bot.username + " Console").color(NamedTextColor.GRAY),
                            Component.text(bot.config.ownerName).color(ColorUtilities.getColorByString(bot.config.colorPalette.ownerName)),
                            Component.text(line).color(NamedTextColor.GRAY)
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }
    }
}
