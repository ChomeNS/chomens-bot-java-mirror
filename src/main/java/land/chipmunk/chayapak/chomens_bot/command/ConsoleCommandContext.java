package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConsoleCommandContext extends CommandContext {
    private final Bot bot;

    public ConsoleCommandContext (Bot bot, String prefix) {
        super(bot, prefix, bot.players.getBotEntry() /* real */, null, null, false);
        this.bot = bot;
    }

    @Override
    public void sendOutput (Component component) {
        final String message = ComponentUtilities.stringifyAnsi(component);
        bot.logger.info(message);
    }

    @Override
    public Component displayName () {
        return sender.displayName.color(NamedTextColor.YELLOW);
    }
}
