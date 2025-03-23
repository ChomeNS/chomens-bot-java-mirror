package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClearChatNameAnnouncerPlugin implements CommandSpyPlugin.Listener {
    private final Bot bot;

    public ClearChatNameAnnouncerPlugin (Bot bot) {
        this.bot = bot;

        if (!bot.config.announceClearChatUsername) return;

        bot.commandSpy.addListener(this);
    }

    @Override
    public void commandReceived(PlayerEntry sender, String command) {
        if (
                command.equals("/clearchat") ||
                        command.equals("/cc") ||
                        command.equals("/extras:clearchat") ||
                        command.equals("/extras:cc")
        ) {
            bot.chat.tellraw(
                    Component.translatable("%s cleared the chat")
                            .arguments(Component.selector(sender.profile.getName()))
                            .color(NamedTextColor.DARK_GREEN)
            );
        }
    }
}
