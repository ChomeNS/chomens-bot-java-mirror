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

    public WorldPlugin (final Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundLevelChunkWithLightPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundForgetLevelChunkPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundBlockUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundSectionBlocksUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundRespawnPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundRegistryDataPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundSetSimulationDistancePacket t_packet) packetReceived(t_packet);
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        chunks.clear();
    }

    private void worldChanged (final String dimension) {
        final RegistryEntry currentDimension = registry.stream()
                .filter(eachDimension -> eachDimension.getId().asString().equals(dimension))
                .findFirst()
                .orElse(null);

        if (currentDimension == null) return;

        final NbtMap data = currentDimension.getData();

        if (data == null) return;

        minY = data.getInt("min_y");
        maxY = data.getInt("height") + minY;

        for (final Listener listener : listeners) listener.worldChanged(dimension);
    }

    private void packetReceived (final ClientboundRegistryDataPacket packet) {
        if (!packet.getRegistry().value().equals("dimension_type")) return;

        registry = packet.getEntries();
    }

    private void packetReceived (final ClientboundLoginPacket packet) {
        simulationDistance = packet.getSimulationDistance();

        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    private void packetReceived (final ClientboundRespawnPacket packet) {
        worldChanged(packet.getCommonPlayerSpawnInfo().getWorldName().asString());
    }

    private void packetReceived (final ClientboundSetSimulationDistancePacket packet) {
        simulationDistance = packet.getSimulationDistance();
    }

    private void packetReceived (final ClientboundLevelChunkWithLightPacket packet) {
        final ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());
        final ChunkColumn column = new ChunkColumn(bot, pos, packet.getChunkData(), maxY, minY);
        chunks.put(pos, column);
    }

    private void packetReceived (final ClientboundForgetLevelChunkPacket packet) {
        chunks.remove(new ChunkPos(packet.getX(), packet.getZ()));
    }

    private void packetReceived (final ClientboundBlockUpdatePacket packet) {
        final Vector3i position = packet.getEntry().getPosition();
        final int id = packet.getEntry().getBlock();

        setBlock(position.getX(), position.getY(), position.getZ(), id);
    }

    private void packetReceived (final ClientboundSectionBlocksUpdatePacket packet) {
        for (final BlockChangeEntry entry : packet.getEntries()) {
            final Vector3i position = entry.getPosition();
            final int id = entry.getBlock();

            setBlock(position.getX(), position.getY(), position.getZ(), id);
        }
    }

    public ChunkColumn getChunk (final int x, final int z) { return chunks.get(new ChunkPos(x, z)); }

    public ChunkColumn getChunk (final ChunkPos pos) { return chunks.get(pos); }

    public Collection<ChunkColumn> getChunks () { return chunks.values(); }

    public int getBlock (final int x, final int y, final int z) {
        final ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        final ChunkColumn chunk = chunks.get(chunkPos);
        return chunk == null ? 0 : chunks.get(chunkPos).getBlock(x & 15, y, z & 15);
    }

    // should this be public?
    public void setBlock (final int x, final int y, final int z, final int id) {
        final ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (!chunks.containsKey(chunkPos)) return;
        chunks.get(chunkPos).setBlock(x & 15, y, z & 15, id);
    }

    public void addListener (final Listener listener) { listeners.add(listener); }

    public interface Listener {
        default void worldChanged (final String dimension) { }
    }
}
