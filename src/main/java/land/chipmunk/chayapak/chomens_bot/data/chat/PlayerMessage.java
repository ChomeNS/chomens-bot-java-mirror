package land.chipmunk.chayapak.chomens_bot.data.chat;

import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import net.kyori.adventure.text.Component;

public class PlayerMessage {
    public final PlayerEntry sender;
    public final Component displayName;
    public final Component contents;

    public PlayerMessage (
            PlayerEntry sender,
            Component displayName,
            Component contents
    ) {
        this.sender = sender;
        this.displayName = displayName;
        this.contents = contents;
    }
}
