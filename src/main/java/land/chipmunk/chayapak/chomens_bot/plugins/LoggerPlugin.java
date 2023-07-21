package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;

public class LoggerPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    private boolean addedListener = false;

    public boolean logToConsole = true;

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

    public void log (String message) {
        LoggerUtilities.log(bot, message, true, logToConsole);
    }
    public void log (String message, boolean logToFile, boolean logToConsole) {
        LoggerUtilities.log(bot, message, logToFile, logToConsole);
    }

    public void info (String message) {
        if (!logToConsole) return;

        LoggerUtilities.info(bot, message);
    }

    public void error (String message) {
        if (!logToConsole) return;

        LoggerUtilities.info(bot, message);
    }

    public void custom (Component prefix, Component message) {
        if (!logToConsole) return;

        LoggerUtilities.custom(bot, prefix, message);
    }

    @Override
    public void systemMessageReceived(Component component) {
        final String string = ComponentUtilities.stringify(component);

        if (logToConsole) {
            final String ansi = ComponentUtilities.stringifyAnsi(component);

            log(ansi, false, true);
        }
        log(string, true, false);
    }
}
