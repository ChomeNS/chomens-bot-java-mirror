package me.chayapak1.chomens_bot.selfCares.vanilla.misc;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.InteractAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundPlayerInputPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundInteractPacket;

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

        // automatically dismount when mounting (e.g., player `/ride`s you)
        // the server will automatically unshift for you, which is why we are only sending start shifting
        bot.session.send(
                new ServerboundPlayerInputPacket(
                        false,
                        false,
                        false,
                        false,
                        false,
                        true,
                        false
                )
        );

        // for compatibility with servers <1.21.6, since 1.21.6 removed sneaking stuff from ServerboundPlayerInputPacket
        bot.session.send(
                new ServerboundInteractPacket(
                        -1, // we only need the server to think that we are sneaking, so we don't need to put a valid entity id here
                        InteractAction.INTERACT, // doesn't matter
                        true
                )
        );
    }
}
