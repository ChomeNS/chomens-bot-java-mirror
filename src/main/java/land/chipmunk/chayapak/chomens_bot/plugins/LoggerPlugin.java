package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.FileLoggerUtilities;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    private boolean addedListener = false;

    public boolean logToConsole = true;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public LoggerPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(new Bot.Listener() {
            @Override
            public void connecting() {
                log(
                        String.format(
                                "Connecting to: %s:%s",
                                bot.host,
                                bot.port
                        )
                );
            }

            @Override
            public void connected (ConnectedEvent event) {
                log(
                        String.format(
                                "Successfully connected to: %s:%s",
                                bot.host,
                                bot.port
                        )
                );

                if (addedListener) return;
                bot.chat.addListener(LoggerPlugin.this);
                addedListener = true;
            }

            @Override
            public void disconnected (DisconnectedEvent event) {
                final String reason = ComponentUtilities.stringifyAnsi(event.getReason());
                log("Disconnected from " + bot.host + ":" + bot.port + ", reason: " + reason);
            }
        });
    }

    // ported from chomens bot js
    private String prefix (Component prefix, String _message) {
        LocalDateTime dateTime = LocalDateTime.now();

        final Component message = Component.translatable(
                "[%s %s] [%s] [%s] %s",
                Component.text(dateTime.format(dateTimeFormatter)).color(NamedTextColor.GRAY),
                prefix,
                Component.text(Thread.currentThread().getName()).color(NamedTextColor.GRAY),
                Component.text(bot.options.serverName).color(NamedTextColor.GRAY),
                Component.text(_message).color(NamedTextColor.WHITE)
        ).color(NamedTextColor.DARK_GRAY);

        return ComponentUtilities.stringifyAnsi(message);
    }

    public void log (String message) { log(message, true, logToConsole); }
    public void log (String _message, boolean logToFile, boolean logToConsole) {
        final String message = prefix(Component.text("Log").color(NamedTextColor.GOLD), _message);

        if (logToConsole) bot.console.reader.printAbove(message);
        else if (logToFile) {
            final String formattedMessage = String.format(
                    "[%s] %s",
                    bot.host + ":" + bot.port,
                    _message
            );

            FileLoggerUtilities.log(formattedMessage);
        }
    }

    public void info (String _message) {
        final String message = prefix(Component.text("Info").color(NamedTextColor.GREEN), _message);

        if (logToConsole) bot.console.reader.printAbove(message);
    }

    public void custom (Component prefix, Component _message) {
        final String message = prefix(prefix, ComponentUtilities.stringifyAnsi(_message));

        if (logToConsole) bot.console.reader.printAbove(message);
    }

    @Override
    public void systemMessageReceived(Component component) {
        final String stringMessage = ComponentUtilities.stringify(component);
        final String ansiMessage = ComponentUtilities.stringifyAnsi(component);

        log(ansiMessage, false, true);
        log(stringMessage, true, false);
    }
}
