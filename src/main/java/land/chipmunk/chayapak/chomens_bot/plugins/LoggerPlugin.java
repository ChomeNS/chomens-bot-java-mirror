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

    private int totalConnects = 0;

    public LoggerPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(new Bot.Listener() {
            @Override
            public void connecting() {
                totalConnects++;

                if (totalConnects > 20) return;
                else if (totalConnects == 20) {
                    log("Suspending connecting and disconnect messages from now on");

                    return;
                }

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

                totalConnects = 0;

                if (addedListener) return;
                bot.chat.addListener(LoggerPlugin.this);
                addedListener = true;
            }

            @Override
            public void disconnected (DisconnectedEvent event) {
                if (totalConnects >= 20) return;

                final Component reason = event.getReason();

                final String message = "Disconnected from " + bot.host + ":" + bot.port + ", reason: ";

                final String string = ComponentUtilities.stringify(reason);

                if (logToConsole) {
                    final String ansi = ComponentUtilities.stringifyAnsi(reason);

                    log(message + ansi, false, true);
                }
                log(message + string, true, false);
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
    public boolean systemMessageReceived(Component component, String string, String ansi) {
        if (logToConsole) {
            log(ansi, false, true);
        }

        log(string, true, false);

        return true;
    }
}
