package me.chayapak1.chomens_bot.chomeNSMod;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundCoreOutputPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCoreCommandPacket;
import me.chayapak1.chomens_bot.command.contexts.ChomeNSModCommandContext;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;

public class PacketHandler {
    private final Bot bot;

    public PacketHandler (final Bot bot) {
        this.bot = bot;
    }

    public void handlePacket (final PlayerEntry player, final Packet packet) {
        if (packet instanceof final ServerboundRunCoreCommandPacket t_packet) handlePacket(player, t_packet);
        else if (packet instanceof final ServerboundRunCommandPacket t_packet) handlePacket(player, t_packet);
    }

    private void handlePacket (final PlayerEntry player, final ServerboundRunCoreCommandPacket packet) {
        final CompletableFuture<Component> future = bot.core.runTracked(packet.command);

        if (future == null) {
            bot.chomeNSMod.send(
                    player,
                    new ClientboundCoreOutputPacket(
                            packet.runID,
                            Component.empty()
                    )
            );

            return;
        }

        future.thenApply(output -> {
            bot.chomeNSMod.send(
                    player,
                    new ClientboundCoreOutputPacket(
                            packet.runID,
                            output
                    )
            );

            return null;
        });
    }

    private void handlePacket (final PlayerEntry player, final ServerboundRunCommandPacket packet) {
        final String input = packet.input; // the input is raw, no prefix included

        final ChomeNSModCommandContext context = new ChomeNSModCommandContext(
                bot,
                player
        );

        bot.commandHandler.executeCommand(input, context, null);
    }
}
