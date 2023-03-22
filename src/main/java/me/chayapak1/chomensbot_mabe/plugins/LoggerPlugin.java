package me.chayapak1.chomensbot_mabe.plugins;

import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.util.ComponentUtilities;
import net.kyori.adventure.text.Component;

public class LoggerPlugin extends ChatPlugin.ChatListener {
    private final Bot bot;

    public LoggerPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(new SessionAdapter() {
            @Override
            public void connected (ConnectedEvent event) {
                log("Successfully connected to: " + bot.host() + ":" + bot.port());
            }

            @Override
            public void disconnected (DisconnectedEvent event) {
                log("Disconnected from " + bot.host() + ":" + bot.port() + ", reason: " + event.getReason());
            }
        });

        bot.chat().addListener(this);
    }

    public void log (String message) {
        bot.console().reader().printAbove(
                String.format(
                        "[%s] %s",
                        bot.host() + ":" + bot.port(),
                        message
                )
        );
    }

    @Override
    public void systemMessageReceived(String message, Component component) {
        final String ansiMessage = ComponentUtilities.stringifyAnsi(component);
        log(ansiMessage);
    }
}
