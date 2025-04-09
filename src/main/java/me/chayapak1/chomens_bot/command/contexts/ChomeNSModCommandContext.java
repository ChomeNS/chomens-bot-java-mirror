package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundMessagePacket;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

public class ChomeNSModCommandContext extends CommandContext {
    public ChomeNSModCommandContext (final Bot bot, final PlayerEntry sender) {
        super(
                bot,
                ".cbot ", // intentionally hardcoded
                sender,
                true
        );
    }

    @Override
    public void sendOutput (final Component component) {
        bot.chomeNSMod.send(
                sender,
                new ClientboundMessagePacket(component)
        );
    }

    @Override
    public Component displayName () {
        return sender.displayName;
    }
}
