package land.chipmunk.chayapak.chomens_bot.data.chat;

import net.kyori.adventure.text.Component;

public class PlayerMessage {
    public final MutablePlayerListEntry sender;
    public final Component displayName;
    public final Component contents;

    public PlayerMessage (
            MutablePlayerListEntry sender,
            Component displayName,
            Component contents
    ) {
        this.sender = sender;
        this.displayName = displayName;
        this.contents = contents;
    }
}
