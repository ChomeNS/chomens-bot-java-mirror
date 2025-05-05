package me.chayapak1.chomens_bot.selfCares.vanilla.misc;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.util.Arrays;

public class RideSelfCare extends SelfCare {
    public RideSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return false;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundSetPassengersPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundSetPassengersPacket packet) {
        final int entityId = bot.selfCare.data.entityId;

        if (
                Arrays.stream(packet.getPassengerIds())
                        .noneMatch(id -> id == entityId)
        ) return;

        // automatically unmount when mounting
        // e.g. player `/ride`s you
        // the server will automatically dismount for you,
        // so that's why we don't have to send PlayerState.STOP_SNEAKING
        bot.session.send(
                new ServerboundPlayerCommandPacket(
                        entityId,
                        PlayerState.START_SNEAKING
                )
        );
    }
}
