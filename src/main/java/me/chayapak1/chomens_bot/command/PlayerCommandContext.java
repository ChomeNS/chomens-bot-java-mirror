package me.chayapak1.chomens_bot.command;

import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;

public class PlayerCommandContext extends CommandContext {
    @Getter private final String playerName;

    private final Bot bot;

    public PlayerCommandContext (Bot bot, String playerName, String hash, String ownerHash) {
        super(bot, hash, ownerHash);
        this.bot = bot;
        this.playerName = playerName;
    }

    @Override
    public void sendOutput (Component message) {
        bot.chat().tellraw(message);
    }

    @Override
    public Component displayName () {
        return Component.text(playerName);
    }
}
