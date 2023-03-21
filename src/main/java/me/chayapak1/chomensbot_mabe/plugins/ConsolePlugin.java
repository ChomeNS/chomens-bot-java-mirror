package me.chayapak1.chomensbot_mabe.plugins;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.command.ConsoleCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;

import java.util.Arrays;
import java.util.List;

public class ConsolePlugin {
    private final List<Bot> allBots;

    @Getter public final LineReader reader;

    @Getter private String consoleServer = "all";

    @Getter private final String prefix = ".";
    @Getter private final String consoleServerPrefix = "/";

    public ConsolePlugin (List<Bot> allBots) {
        this.allBots = allBots;
        this.reader = LineReaderBuilder.builder().build();

        for (Bot bot : allBots) {
            bot.console(this);
            bot.logger(new LoggerPlugin(bot));
        }

        String prompt = "> ";

        new Thread(() -> {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt);
                } catch (NoClassDefFoundError e) {
                    line = reader.readLine(prompt);
                } catch (EndOfFileException e) {
                    return;
                } catch (Exception e) {
                    System.exit(1);
                }

                handleLine(line);
            }
        }).start();
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
                        bot.logger().log("No server specified");
                        return;
                    }
                    consoleServer = args[0];
                    bot.logger().log("Set the console server to " + consoleServer);
                }
            }

            return;
        }

        for (Bot bot : allBots) {
            if (!bot.host().equals(consoleServer) && !consoleServer.equals("all")) continue;

            if (line.startsWith(prefix)) {
                final ConsoleCommandContext context = new ConsoleCommandContext(bot, "h", "o"); // ? should the hashes be hardcoded?

                final Component output = bot.commandHandler().executeCommand(line.substring(prefix.length()), context, "h", "o");
                final String textOutput = ((TextComponent) output).content();

                if (!textOutput.equals("success")) {
                    context.sendOutput(output);
                }

                continue;
            }

            bot.chat().tellraw(
                    Component.translatable(
                            "[%s] %s › %s",
                            Component.text(bot.username() + " Console").color(NamedTextColor.GRAY),
                            Component.text("chayapak").color(NamedTextColor.GREEN),
                            Component.text(line).color(NamedTextColor.GRAY)
                    ).color(NamedTextColor.DARK_GRAY)
            );
        }
    }
}
