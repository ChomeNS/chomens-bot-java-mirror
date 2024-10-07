package me.chayapak1.chomens_bot.plugins;

import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
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

    public List<RegistryEntry> registry = null;

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

    private void worldChanged (String dimension) {
        final RegistryEntry currentDimension = registry.stream()
                .filter(eachDimension -> eachDimension.getId().asString().equals(dimension))
                .toArray(RegistryEntry[]::new)[0];

        final NbtMap data = currentDimension.getData();

        if (data == null) return;

        minY = data.getInt("min_y");
        maxY = data.getInt("height");

        for (Listener listener : listeners) listener.worldChanged(dimension);
    }

    public void packetReceived (ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().value().equals("dimension_type")) return;

        registry = packet.getEntries();
    }

    public void packetReceived (ClientboundLoginPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    public void packetReceived (ClientboundRespawnPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    public static class Listener {
        public void worldChanged (String dimension) {}
    }
}
