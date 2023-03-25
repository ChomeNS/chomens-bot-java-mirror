package me.chayapak1.chomens_bot.command;

import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;

public class CommandContext {
    @Getter public final Bot bot;

    @Getter public final String prefix;

    @Getter private final String hash;
    @Getter private final String ownerHash;

    public CommandContext(Bot bot, String prefix, String hash, String ownerHash) {
        this.bot = bot;
        this.prefix = prefix;
        this.hash = hash;
        this.ownerHash = ownerHash;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
