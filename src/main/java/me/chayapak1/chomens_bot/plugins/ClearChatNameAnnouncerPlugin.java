package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClearChatNameAnnouncerPlugin implements Listener {
    private final Bot bot;

    public ClearChatNameAnnouncerPlugin (final Bot bot) {
        this.bot = bot;

        if (!bot.config.announceClearChatUsername) return;

        bot.listener.addListener(this);
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        if (!bot.config.announceClearChatUsername) return;

        if (
                command.equals("/clearchat")
                        || command.equals("/cc")
                        || command.equals("/extras:clearchat")
                        || command.equals("/extras:cc")
        ) {
            bot.chat.tellraw(
                    Component.translatable(
                            "%s cleared the chat", NamedTextColor.DARK_GREEN,
                            Component.selector(sender.profile.getIdAsString())
                    )
            );
        }
    }
}
