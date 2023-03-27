package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.PlayerMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

// idea totallynotskidded™ from chipmunkbot (the js one)
public class AntiClearChatPlugin extends ChatPlugin.ChatListener {
    private final Bot bot;

    public AntiClearChatPlugin (Bot bot) {
        this.bot = bot;

        bot.chat().addListener(this);
    }

    @Override
    public void commandSpyMessageReceived (PlayerMessage message) {
        final String username = message.sender().profile().getName();
        final String command = ((TextComponent) message.parameters().get("contents")).content();

        if (
                command.equals("/clearchat") ||
                        command.equals("/cc") ||
                        command.equals("/extras:clearchat") ||
                        command.equals("/extras:cc")
        ) {
            bot.chat().tellraw(
                    Component.empty()
                            .append(Component.text(username))
                            .append(Component.text(" cleared the chat"))
                            .color(NamedTextColor.DARK_GREEN)
            );
        }
    }
}
