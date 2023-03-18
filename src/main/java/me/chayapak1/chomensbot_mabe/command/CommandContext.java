package me.chayapak1.chomensbot_mabe.command;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import net.kyori.adventure.text.Component;

public class CommandContext {
    @Getter public final Bot bot;

    public CommandContext(Bot bot) {
        this.bot = bot;
    }

    public Component displayName () { return Component.empty(); }
    public void sendOutput (Component component) {}
}
