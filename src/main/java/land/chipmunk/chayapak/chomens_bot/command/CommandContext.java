package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import lombok.Getter;
import net.kyori.adventure.text.Component;

public class CommandContext {
    @Getter private final Bot bot;

    @Getter private final String prefix;

    @Getter private final MutablePlayerListEntry sender;

    @Getter private final String hash;
    @Getter private final String ownerHash;

    @Getter private final boolean inGame;

    public CommandContext(Bot bot, String prefix, MutablePlayerListEntry sender, String hash, String ownerHash, boolean inGame) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.hash = hash;
        this.ownerHash = ownerHash;
        this.inGame = inGame;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
