package me.chayapak1.chomens_bot.data.chat;

import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

public record PlayerMessage (PlayerEntry sender, Component displayName, Component contents) {
    @Override
    public String toString() {
        return "PlayerMessage{" +
                "sender=" + sender +
                ", displayName=" + displayName +
                ", contents=" + contents +
                '}';
    }
}
