package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chunk.ChunkColumn;
import me.chayapak1.chomens_bot.data.chunk.ChunkPos;
import me.chayapak1.chomens_bot.data.listener.Listener;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class WorldPlugin implements Listener {
    private final Bot bot;

    public int minY = 0;
    public int maxY = 256;

    public int simulationDistance = 8;

    public String currentDimension = "";

    private final Map<ChunkPos, ChunkColumn> chunks = new Object2ObjectOpenHashMap<>();

    public List<RegistryEntry> registry = null;

    public WorldPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        switch (packet) {
            case final ClientboundLevelChunkWithLightPacket t_packet -> packetReceived(t_packet);
            case final ClientboundForgetLevelChunkPacket t_packet -> packetReceived(t_packet);
            case final ClientboundBlockUpdatePacket t_packet -> packetReceived(t_packet);
            case final ClientboundSectionBlocksUpdatePacket t_packet -> packetReceived(t_packet);
            case final ClientboundLoginPacket t_packet -> packetReceived(t_packet);
            case final ClientboundRespawnPacket t_packet -> packetReceived(t_packet);
            case final ClientboundRegistryDataPacket t_packet -> packetReceived(t_packet);
            case final ClientboundSetSimulationDistancePacket t_packet -> packetReceived(t_packet);
            default -> { }
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        chunks.clear();
    }

    private void worldChanged (final String dimension) {
        currentDimension = dimension;

        final RegistryEntry currentDimension = registry.stream()
                .filter(eachDimension -> eachDimension.getId().asString().equals(dimension))
                .findFirst()
                .orElse(null);

        if (currentDimension == null) return;

        final NbtMap data = currentDimension.getData();

        if (data == null) return;

        minY = data.getInt("min_y");
        maxY = data.getInt("height") + minY;

        bot.listener.dispatch(listener -> listener.onWorldChanged(dimension));
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
        final ChunkColumn column = new ChunkColumn(pos, packet.getChunkData(), maxY, minY);
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

        try {
            return chunk == null ? 0 : chunks.get(chunkPos).getBlock(x & 15, y, z & 15);
        } catch (final Exception e) {
            return 0;
        }
    }

    // should this be public?
    public void setBlock (final int x, final int y, final int z, final int id) {
        final ChunkPos chunkPos = new ChunkPos(Math.floorDiv(x, 16), Math.floorDiv(z, 16));
        if (!chunks.containsKey(chunkPos)) return;
        chunks.get(chunkPos).setBlock(x & 15, y, z & 15, id);
    }
}
