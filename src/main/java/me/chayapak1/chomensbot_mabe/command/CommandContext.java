package me.chayapak1.chomensbot_mabe.command;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import net.kyori.adventure.text.Component;

public class CommandContext {
    @Getter public final Bot bot;

    @Getter private final String hash;
    @Getter private final String ownerHash;

    public CommandContext(Bot bot, String hash, String ownerHash) {
        this.bot = bot;
        this.hash = hash;
        this.ownerHash = ownerHash;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
