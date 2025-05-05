package me.chayapak1.chomens_bot.selfCares.vanilla.misc;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.selfCare.SelfCare;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;

public class OpenScreenSelfCare extends SelfCare {
    public OpenScreenSelfCare (final Bot bot) {
        super(bot);
    }

    @Override
    public boolean shouldRun () {
        return false;
    }

    @Override
    public void onPacketReceived (final Packet packet) {
        if (packet instanceof final ClientboundOpenScreenPacket t_packet) onPacketReceived(t_packet);
    }

    private void onPacketReceived (final ClientboundOpenScreenPacket packet) {
        // instantly closes the window (like `/craft`, `/ec`, etc.)
        bot.session.send(new ServerboundContainerClosePacket(packet.getContainerId()));
    }
}
