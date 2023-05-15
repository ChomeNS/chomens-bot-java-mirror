package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import lombok.Getter;
import land.chipmunk.chayapak.chomens_bot.Bot;
import net.kyori.adventure.text.Component;

public class PlayerCommandContext extends CommandContext {
    @Getter private final String playerName;

    @Getter private final String selector;

    private final Bot bot;

    public PlayerCommandContext (Bot bot, String playerName, String prefix, String selector, MutablePlayerListEntry sender, String hash, String ownerHash) {
        super(bot, prefix, sender, hash, ownerHash, true);
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
        return sender().displayName();
    }
}
