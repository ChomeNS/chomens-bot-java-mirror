package land.chipmunk.chayapak.chomens_bot.command;

import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.Bot;
import net.kyori.adventure.text.Component;

public class PlayerCommandContext extends CommandContext {
    public final String playerName;

    public final String selector;

    private final Bot bot;

    public PlayerCommandContext (Bot bot, String playerName, String prefix, String selector, PlayerEntry sender) {
        super(bot, prefix, sender, true);
        this.bot = bot;
        this.playerName = playerName;
        this.selector = selector;
    }

    @Override
    public void sendOutput (Component message) {
        bot.chat.tellraw(
                Component.translatable(
                        "%s",
                        message,
                        Component.text("chomens_bot_command_output" + ((commandName != null) ? "_" + commandName : ""))
                ),
                selector
        );
    }

    @Override
    public Component displayName () {
        return sender.displayName;
    }
}
