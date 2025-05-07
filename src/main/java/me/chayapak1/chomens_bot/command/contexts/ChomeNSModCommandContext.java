package me.chayapak1.chomens_bot.command.contexts;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundMessagePacket;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.I18nUtilities;
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
        final Component rendered = I18nUtilities.render(component);
        bot.chomeNSMod.send(
                sender,
                new ClientboundMessagePacket(rendered)
        );
    }

    @Override
    public Component displayName () {
        return sender.displayName;
    }
}
