package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundCommandOutputPacket;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

public class ChomeNSModCommandContext extends CommandContext {
    public ChomeNSModCommandContext (Bot bot, PlayerEntry sender) {
        super(
                bot,
                ".cbot ", // intentionally hardcoded
                sender,
                true
        );
    }

    @Override
    public void sendOutput (Component component) {
        bot.chomeNSMod.send(
                sender,
                new ClientboundCommandOutputPacket(component)
        );
    }

    @Override
    public Component displayName () {
        return sender.displayName;
    }
}
