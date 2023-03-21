package me.chayapak1.chomensbot_mabe.command;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConsoleCommandContext extends CommandContext {
    private final Bot bot;

    public ConsoleCommandContext (Bot bot, String hash, String ownerHash) {
        super(bot, hash, ownerHash);
        this.bot = bot;
    }

    @Override
    public void sendOutput (Component component) {
        final String message = ComponentUtilities.stringify(component);
        bot.logger().log(message);
    }

    @Override
    public Component displayName () {
        return Component.text(bot.username()).color(NamedTextColor.YELLOW);
    }
}
