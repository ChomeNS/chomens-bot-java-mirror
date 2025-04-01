package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chunk.ChunkColumn;
import me.chayapak1.chomens_bot.data.chunk.ChunkPos;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.RegistryEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundRegistryDataPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundRespawnPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.*;

import java.util.*;

public class WorldPlugin extends Bot.Listener {
    private final Bot bot;

    public int minY = 0;
    public int maxY = 256;

    public int simulationDistance = 8;

    private final Map<ChunkPos, ChunkColumn> chunks = new HashMap<>();

    public List<RegistryEntry> registry = null;

    private final List<Listener> listeners = new ArrayList<>();

    public WorldPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundLevelChunkWithLightPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundForgetLevelChunkPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundBlockUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundSectionBlocksUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRespawnPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRegistryDataPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundSetSimulationDistancePacket t_packet) packetReceived(t_packet);
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        chunks.clear();
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

    private void packetReceived (ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().value().equals("dimension_type")) return;

        registry = packet.getEntries();
    }

    private void packetReceived (ClientboundLoginPacket packet) {
        simulationDistance = packet.getSimulationDistance();

        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    private void packetReceived (ClientboundRespawnPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    private void packetReceived (ClientboundSetSimulationDistancePacket packet) {
        simulationDistance = packet.getSimulationDistance();
    }

    private void packetReceived (ClientboundLevelChunkWithLightPacket packet) {
        final ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());
        final ChunkColumn column = new ChunkColumn(bot, pos, packet.getChunkData(), maxY, minY);
        chunks.put(pos, column);
    }

    private void packetReceived (ClientboundForgetLevelChunkPacket packet) {
        chunks.remove(new ChunkPos(packet.getX(), packet.getZ()));
    }

    private void packetReceived (ClientboundBlockUpdatePacket packet) {
        final Vector3i position = packet.getEntry().getPosition();
        final int id = packet.getEntry().getBlock();

        setBlock(position.getX(), position.getY(), position.getZ(), id);
    }

    private void packetReceived (ClientboundSectionBlocksUpdatePacket packet) {
        for (BlockChangeEntry entry : packet.getEntries()) {
            final Vector3i position = entry.getPosition();
            final int id = entry.getBlock();

            setBlock(position.getX(), position.getY(), position.getZ(), id);
        }
    }

    public ChunkColumn getChunk (int x, int z) { return chunks.get(new ChunkPos(x, z)); }

    public ChunkColumn getChunk (ChunkPos pos) { return chunks.get(pos); }

    public Collection<ChunkColumn> getChunks () { return chunks.values(); }

    public int getBlock (int x, int y, int z) {
        final ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        final ChunkColumn chunk = chunks.get(chunkPos);
        return chunk == null ? 0 : chunks.get(chunkPos).getBlock(x & 15, y, z & 15);
    }

    // should this be public?
    public void setBlock (int x, int y, int z, int id) {
        final ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (!chunks.containsKey(chunkPos)) return;
        chunks.get(chunkPos).setBlock(x & 15, y, z & 15, id);
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public interface Listener {
        default void worldChanged (String dimension) { }
    }
}
