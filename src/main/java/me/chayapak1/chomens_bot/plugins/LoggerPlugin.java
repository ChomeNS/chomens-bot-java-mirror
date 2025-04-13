package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.util.ExceptionUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

public class LoggerPlugin implements ChatPlugin.Listener {
    private final Bot bot;

    public boolean logToConsole = true;

    public LoggerPlugin (final Bot bot) {
        this.bot = bot;

        bot.addListener(new Bot.Listener() {
            @Override
            public void connecting () {
                if (bot.connectAttempts > 10) return;
                else if (bot.connectAttempts == 10) {
                    log("Suppressing connection status messages from now on");

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
            public void connected (final ConnectedEvent event) {
                log(
                        String.format(
                                "Successfully connected to: %s",
                                bot.getServerString(true)
                        )
                );
            }

            @Override
            public void disconnected (final DisconnectedEvent event) {
                if (bot.connectAttempts >= 10) return;

                final Component message = Component.translatable(
                        "Disconnected from %s, reason: %s",
                        Component.text(bot.getServerString(true)),
                        event.getReason()
                );

                log(message);
            }

            @Override
            public void loadedPlugins (final Bot bot) {
                bot.chat.addListener(LoggerPlugin.this);
            }
        });
    }

    public void log (final LogType type, final Component message) {
        LoggerUtilities.log(type, bot, message, true, logToConsole);
    }

    public void log (final LogType type, final Component message, final boolean logToFile) {
        LoggerUtilities.log(type, bot, message, logToFile, logToConsole);
    }

    public void log (final Component message) { log(LogType.INFO, message); }

    public void log (final String message) { log(LogType.INFO, Component.text(message)); }

    public void log (final LogType type, final String message) { log(type, Component.text(message)); }

    public void error (final Component message) { log(LogType.ERROR, message); }

    public void error (final String message) { log(LogType.ERROR, Component.text(message)); }

    public void error (final Throwable throwable) { log(LogType.ERROR, ExceptionUtilities.getStacktrace(throwable)); }

    @Override
    public boolean systemMessageReceived (final Component component, final String string, final String ansi) {
        log(LogType.CHAT, component);

        return true;
    }
}
