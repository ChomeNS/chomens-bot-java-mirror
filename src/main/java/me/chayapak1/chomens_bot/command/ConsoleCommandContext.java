package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConsoleCommandContext extends CommandContext {
    private final Bot bot;

    public ConsoleCommandContext (Bot bot, String prefix, String hash, String ownerHash) {
        super(bot, prefix, bot.players().getEntry(bot.username()) /* real */, hash, ownerHash);
        this.bot = bot;
    }

    @Override
    public void sendOutput (Component component) {
        final String message = ComponentUtilities.stringifyAnsi(component);
        bot.logger().log(message);
    }

    @Override
    public Component displayName () {
        return Component.text(bot.username()).color(NamedTextColor.YELLOW);
    }
}
