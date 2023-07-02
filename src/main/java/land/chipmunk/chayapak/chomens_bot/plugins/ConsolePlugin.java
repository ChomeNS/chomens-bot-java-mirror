package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.ConsoleCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.dv8tion.jda.api.JDA;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsolePlugin {
    private final List<Bot> allBots;

    public final LineReader reader;

    public String consoleServer = "all";

    public String prefix;
    public String consoleServerPrefix;

    private static final List<Listener> listeners = new ArrayList<>();

    public ConsolePlugin (List<Bot> allBots, Configuration discordConfig, JDA jda) {
        this.allBots = allBots;
        this.reader = LineReaderBuilder.builder().build();

        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        for (Bot bot : allBots) {
            prefix = bot.config.consolePrefixes.normalCommandsPrefix;
            consoleServerPrefix = bot.config.consolePrefixes.consoleServerPrefix;

            bot.console = this;

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

    public void handleLine (String line) {
        if (line == null) return;

        if (line.startsWith(consoleServerPrefix)) {
            final String substringLine = line.substring(consoleServerPrefix.length());
            final String[] splitInput = substringLine.split("\\s+");

            final String commandName = splitInput[0];
            final String[] args = Arrays.copyOfRange(splitInput, 1, splitInput.length);

            if (commandName.equals("csvr") || commandName.equals("consoleserver")) {
                final List<String> servers = new ArrayList<>();

                for (Bot bot : allBots) {
                    servers.add(bot.host + ":" + bot.port);
                }

                for (Bot bot : allBots) {
                    if (args.length == 0) {
                        bot.logger.info("No server specified");
                        return;
                    }

                    if (String.join(" ", args).equalsIgnoreCase("all")) {
                        consoleServer = "all";
                        bot.logger.info("Set the console server to all servers");
                        return;
                    }
                    try {
                        // servers.find(server => server.toLowerCase().includes(args.join(' '))) in js i guess
                        consoleServer = servers.stream()
                                .filter(server -> server.toLowerCase().contains(String.join(" ", args)))
                                .toArray(String[]::new)[0];

                        bot.logger.info("Set the console server to " + String.join(", ", consoleServer));
                    } catch (ArrayIndexOutOfBoundsException e) {
                        bot.logger.info("Invalid server: " + String.join(" ", args));
                    }
                }
            }

            return;
        }

        for (Bot bot : allBots) {
            final String hostAndPort = bot.host + ":" + bot.port;

            if (!hostAndPort.equals(consoleServer) && !consoleServer.equals("all")) continue;

            if (line.startsWith(prefix)) {
                final ConsoleCommandContext context = new ConsoleCommandContext(bot, prefix);

                final Component output = bot.commandHandler.executeCommand(line.substring(prefix.length()), context, false, false, true, null);

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

    public static void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void ready () {}
    }
}
