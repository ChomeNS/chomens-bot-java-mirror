package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import net.kyori.adventure.text.Component;

public class CommandContext {
    public final Bot bot;

    public final String prefix;

    public final PlayerEntry sender;

    public final boolean inGame;

    public String[] splitInput;

    public CommandContext(Bot bot, String prefix, PlayerEntry sender, boolean inGame) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.inGame = inGame;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
