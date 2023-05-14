package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import land.chipmunk.chayapak.chomens_bot.command.ConsoleCommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import lombok.Getter;
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

    @Getter public final LineReader reader;

    @Getter private String consoleServer = "all";

    @Getter private String prefix;
    @Getter private String consoleServerPrefix;

    private static final List<Listener> listeners = new ArrayList<>();

    public ConsolePlugin (List<Bot> allBots, Configuration discordConfig, JDA jda) {
        this.allBots = allBots;
        this.reader = LineReaderBuilder.builder().build();

        reader.option(LineReader.Option.DISABLE_EVENT_EXPANSION, true);

        for (Bot bot : allBots) {
            prefix = bot.config().consolePrefixes().get("normalCommandsPrefix");
            consoleServerPrefix = bot.config().consolePrefixes().get("consoleServerPrefix");

            bot.console(this);

            bot.logger(new LoggerPlugin(bot));
        }

        new DiscordPlugin(discordConfig, jda);

        final String prompt = "> ";

        new Thread(() -> {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt);
                } catch (Exception e) {
                    System.exit(1);
                }

                handleLine(line);
            }
        }).start();

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
                for (Bot bot : allBots) {
                    if (args.length == 0) {
                        bot.logger().info("No server specified");
                        return;
                    }
                    consoleServer = args[0];
                    bot.logger().info("Set the console server to " + consoleServer);
                }
            }

            return;
        }

        for (Bot bot : allBots) {
            if (!bot.host().equals(consoleServer) && !consoleServer.equals("all")) continue;

            if (line.startsWith(prefix)) {
                final ConsoleCommandContext context = new ConsoleCommandContext(bot, prefix);

                final Component output = bot.commandHandler().executeCommand(line.substring(prefix.length()), context, false, false, true, null, null, null);

                if (output != null) {
                    context.sendOutput(output);
                }

                continue;
            }

            bot.chat().tellraw(
                    Component.translatable(
                            "[%s] %s › %s",
                            Component.text(bot.username() + " Console").color(NamedTextColor.GRAY),
                            Component.text(bot.config().ownerName()).color(ColorUtilities.getColorByString(bot.config().colorPalette().ownerName())),
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
