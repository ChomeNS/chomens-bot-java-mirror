package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WorldPlugin extends Bot.Listener {
    private final Bot bot;

    public int minY = 0;
    public int maxY = 256;

    public CompoundTag registry = null;

    private final List<Listener> listeners = new ArrayList<>();

    public WorldPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
        else if (packet instanceof ClientboundRespawnPacket) packetReceived((ClientboundRespawnPacket) packet);
        else if (packet instanceof ClientboundRegistryDataPacket) packetReceived((ClientboundRegistryDataPacket) packet);
    }

    @SuppressWarnings("unchecked")
    private void worldChanged (String dimension) {
        final Tag dimensionType = ((LinkedHashMap<String, Tag>) registry.get("minecraft:dimension_type").getValue()).get("value");

        final ArrayList<CompoundTag> dimensions = (ArrayList<CompoundTag>) dimensionType.getValue();

        final CompoundTag currentDimension = dimensions.stream()
                .filter((eachDimension) -> eachDimension.get("name").getValue().equals(dimension))
                .toArray(CompoundTag[]::new)[0];

        final CompoundTag element = currentDimension.get("element");

        minY = (int) element.get("min_y").getValue();
        maxY = (int) element.get("height").getValue();

        for (Listener listener : listeners) listener.worldChanged(dimension);
    }

    public void packetReceived (ClientboundRegistryDataPacket packet) {
        registry = packet.getRegistry();
    }

    public void packetReceived (ClientboundLoginPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getDimension());
    }

    public void packetReceived (ClientboundRespawnPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getDimension());
    }

    public static class Listener {
        public void worldChanged (String dimension) {}
    }
}
