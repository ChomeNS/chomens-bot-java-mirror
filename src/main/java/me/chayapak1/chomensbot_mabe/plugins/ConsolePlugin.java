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
import org.jline.reader.UserInterruptException;

public class ConsolePlugin {
    private final Bot bot;

    @Getter public final LineReader reader;

    @Getter private final String prefix = ".";

    public ConsolePlugin (Bot bot) {
        this.bot = bot;

        this.reader = LineReaderBuilder.builder().build();
        String prompt = "> ";

        new Thread(() -> {
            while (true) {
                String line = null;
                try {
                    line = reader.readLine(prompt);
                } catch (UserInterruptException e) {
                    System.exit(1); // yup
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

        if (line.startsWith(prefix)) {
            final ConsoleCommandContext context = new ConsoleCommandContext(bot);

            final Component output = CommandHandlerPlugin.executeCommand(line.substring(prefix.length()), context);
            final String textOutput = ((TextComponent) output).content();

            if (!textOutput.equals("success")) {
                context.sendOutput(output);
            }

            return;
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
