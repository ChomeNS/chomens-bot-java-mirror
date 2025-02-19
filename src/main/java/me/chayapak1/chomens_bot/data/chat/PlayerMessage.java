package me.chayapak1.chomens_bot.data.chat;

import me.chayapak1.chomens_bot.data.player.PlayerEntry;
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

    @Override
    public String toString() {
        return "PlayerMessage{" +
                "sender=" + sender +
                ", displayName=" + displayName +
                ", contents=" + contents +
                '}';
    }
}
