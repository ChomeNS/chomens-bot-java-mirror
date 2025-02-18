package me.chayapak1.chomens_bot.plugins;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import me.chayapak1.chomens_bot.Bot;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetSimulationDistancePacket;

import java.util.ArrayList;
import java.util.List;

public class WorldPlugin extends Bot.Listener {
    public int minY = 0;
    public int maxY = 256;

    public int simulationDistance = 8;

    public List<RegistryEntry> registry = null;

    private final List<Listener> listeners = new ArrayList<>();

    public WorldPlugin (Bot bot) {
        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRespawnPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRegistryDataPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundSetSimulationDistancePacket t_packet) packetReceived(t_packet);
    }

    private void worldChanged (String dimension) {
        final RegistryEntry currentDimension = registry.stream()
                .filter(eachDimension -> eachDimension.getId().asString().equals(dimension))
                .findFirst()
                .orElse(null);

        if (currentDimension == null) return;

        final NbtMap data = currentDimension.getData();

        if (data == null) return;

        minY = data.getInt("min_y");
        maxY = data.getInt("height") + minY;

        for (Listener listener : listeners) listener.worldChanged(dimension);
    }

    public void packetReceived (ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().value().equals("dimension_type")) return;

        registry = packet.getEntries();
    }

    public void packetReceived (ClientboundLoginPacket packet) {
        simulationDistance = packet.getSimulationDistance();

        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    public void packetReceived (ClientboundRespawnPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    public void packetReceived (ClientboundSetSimulationDistancePacket packet) {
        simulationDistance = packet.getSimulationDistance();
    }

    public static class Listener {
        public void worldChanged (String dimension) {}
    }
}
