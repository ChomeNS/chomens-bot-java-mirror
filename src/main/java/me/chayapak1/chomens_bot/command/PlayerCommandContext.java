package me.chayapak1.chomens_bot.command;

import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;

public class PlayerCommandContext extends CommandContext {
    @Getter private final String playerName;

    @Getter private final String selector;

    private final Bot bot;

    public PlayerCommandContext (Bot bot, String playerName, String prefix, String selector, String hash, String ownerHash) {
        super(bot, prefix, hash, ownerHash);
        this.bot = bot;
        this.playerName = playerName;
        this.selector = selector;
    }

    @Override
    public void sendOutput (Component message) {
        bot.chat().tellraw(message, selector);
    }

    @Override
    public Component displayName () {
        return Component.text(playerName);
    }
}
