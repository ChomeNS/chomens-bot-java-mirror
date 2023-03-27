package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.List;

public class PositionPlugin extends SessionAdapter {
    private final Bot bot;

    private final List<PositionListener> listeners = new ArrayList<>();

    @Getter private Vector3i position = Vector3i.from(0, 0, 0);

    public PositionPlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerPositionPacket) {
            packetReceived((ClientboundPlayerPositionPacket) packet);
        }
    }

    public void packetReceived (ClientboundPlayerPositionPacket packet) {
        bot.session().send(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));

        position = Vector3i.from(packet.getX(), packet.getY(), packet.getZ());
        for (PositionListener listener : listeners) { listener.positionChange(position); }
    }

    public void addListener (PositionListener listener) { listeners.add(listener); }

    public static class PositionListener {
        public void positionChange (Vector3i position) {}
    }
}
