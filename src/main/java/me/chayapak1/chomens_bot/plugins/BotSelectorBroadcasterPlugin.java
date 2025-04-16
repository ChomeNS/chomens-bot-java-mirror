package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;

// i don't really want to give players the exact bot selector,
// since that can actually be used to harm the bot,
// but it's used for the command suggestions
// (not sure what are some other ways to do it)
public class BotSelectorBroadcasterPlugin implements Listener {
    private final Bot bot;

    private final String id;

    public BotSelectorBroadcasterPlugin (final Bot bot) {
        this.bot = bot;
        this.id = bot.config.namespace + "_selector"; // chomens_bot_selector, for example

        bot.listener.addListener(this);
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        sendSelector(UUIDUtilities.selector(target.profile.getId()));
    }

    @Override
    public void onCoreReady () {
        sendSelector("@a");
    }

    private void sendSelector (final String playerSelector) {
        bot.chat.actionBar(
                Component.translatable(
                        "",
                        Component.text(id),
                        Component.text(UUIDUtilities.selector(bot.profile.getId()))
                ),
                playerSelector
        );
    }
}
