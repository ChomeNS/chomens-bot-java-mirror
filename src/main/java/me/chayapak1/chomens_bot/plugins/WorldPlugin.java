package me.chayapak1.chomens_bot.plugins;

import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import me.chayapak1.chomens_bot.Bot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WorldPlugin extends Bot.Listener {
    private final Bot bot;

    public int minY = 0;
    public int maxY = 256;

    public Key registry = null;

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

//    @SuppressWarnings("unchecked")
    private void worldChanged (int dimension) {
        // FIXME
        bot.logger.info(registry.value());
        bot.logger.info(registry.toString());
//        final Key dimensionType = ((LinkedHashMap<String, Key>) registry.get("minecraft:dimension_type").asString()).get("value");
//
//        final ArrayList<Key> dimensions = (ArrayList<Key>) dimensionType.asString();
//
//        final Key currentDimension = dimensions.stream()
//                .filter((eachDimension) -> eachDimension.get("name").getValue().equals(dimension))
//                .toArray(Key[]::new)[0];
//
//        final Key element = currentDimension.get("element");
//
//        minY = (int) element.get("min_y").getValue();
//        maxY = (int) element.get("height").getValue();
//
//        for (Listener listener : listeners) listener.worldChanged(dimension);
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
