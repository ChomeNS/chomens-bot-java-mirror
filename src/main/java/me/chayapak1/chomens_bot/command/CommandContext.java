package me.chayapak1.chomens_bot.command;

import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import net.kyori.adventure.text.Component;

public class CommandContext {
    @Getter private final Bot bot;

    @Getter private final String prefix;

    @Getter private final MutablePlayerListEntry sender;

    @Getter private final String hash;
    @Getter private final String ownerHash;

    public CommandContext(Bot bot, String prefix, MutablePlayerListEntry sender, String hash, String ownerHash) {
        this.bot = bot;
        this.prefix = prefix;
        this.sender = sender;
        this.hash = hash;
        this.ownerHash = ownerHash;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
