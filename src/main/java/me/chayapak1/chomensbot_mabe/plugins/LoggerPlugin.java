package me.chayapak1.chomensbot_mabe.plugins;

import me.chayapak1.chomensbot_mabe.Bot;
import net.kyori.adventure.text.Component;

public class LoggerPlugin extends ChatPlugin.ChatListener {
    private final Bot bot;

    public LoggerPlugin(Bot bot) {
        this.bot = bot;
        bot.chat().addListener(this);
    }

    public void log (String message) {
        bot.console().reader().printAbove(message);
    }

    @Override
    public void systemMessageReceived(String message, Component component) {
        log(message);
    }
}
