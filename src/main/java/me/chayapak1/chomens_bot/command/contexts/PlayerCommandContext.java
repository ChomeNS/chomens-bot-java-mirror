package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

public class PlayerCommandContext extends CommandContext {
    public final String playerName;

    public final String selector;

    public final ChatPacketType packetType;

    private final Bot bot;

    public PlayerCommandContext (
            final Bot bot,
            final String playerName,
            final String prefix,
            final String selector,
            final PlayerEntry sender,
            final ChatPacketType packetType
    ) {
        super(bot, prefix, sender, true);
        this.bot = bot;
        this.playerName = playerName;
        this.selector = selector;
        this.packetType = packetType;
    }

    @Override
    public void sendOutput (final Component message) {
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
