package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.util.ExceptionUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

public class LoggerPlugin implements ChatPlugin.Listener {
    public static void init () {
        for (Bot bot : Main.bots) new LoggerPlugin(bot);
    }

    private final Bot bot;

    public boolean logToConsole = true;

    private int totalConnects = 0;

    public LoggerPlugin (Bot bot) {
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
                                "Connecting to: %s",
                                bot.getServerString(true)
                        )
                );
            }

            @Override
            public void connected (ConnectedEvent event) {
                log(
                        String.format(
                                "Successfully connected to: %s",
                                bot.getServerString(true)
                        )
                );

                totalConnects = 0;
            }

            @Override
            public void disconnected (DisconnectedEvent event) {
                if (totalConnects >= 20) return;

                final Component message = Component.translatable(
                        "Disconnected from %s, reason: %s",
                        Component.text(bot.getServerString(true)),
                        event.getReason()
                );

                log(message);
            }

            @Override
            public void loadedPlugins (Bot bot) {
                bot.chat.addListener(LoggerPlugin.this);
            }
        });

        bot.logger = this;
    }

    public void log (LogType type, Component message) {
        LoggerUtilities.log(type, bot, message, true, logToConsole);
    }

    public void log (LogType type, Component message, boolean logToFile) {
        LoggerUtilities.log(type, bot, message, logToFile, logToConsole);
    }

    public void log (Component message) { log(LogType.INFO, message); }
    public void log (String message) { log(LogType.INFO, Component.text(message)); }
    public void log (LogType type, String message) { log(type, Component.text(message)); }

    public void error (Component message) { log(LogType.ERROR, message); }
    public void error (String message) { log(LogType.ERROR, Component.text(message)); }
    public void error (Throwable throwable) { log(LogType.ERROR, ExceptionUtilities.getStacktrace(throwable)); }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        log(LogType.CHAT, component);

        return true;
    }
}
