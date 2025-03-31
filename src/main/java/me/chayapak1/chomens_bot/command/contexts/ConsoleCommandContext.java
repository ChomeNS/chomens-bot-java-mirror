package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.logging.LogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ConsoleCommandContext extends CommandContext {
    private final Bot bot;

    public ConsoleCommandContext (Bot bot, String prefix) {
        super(bot, prefix, bot.players.getBotEntry() /* real */, false);
        this.bot = bot;
    }

    @Override
    public void sendOutput (Component component) {
        bot.logger.log(LogType.COMMAND_OUTPUT, component);
    }

    @Override
    public Component displayName () {
        return sender.displayName.color(NamedTextColor.YELLOW);
    }
}
