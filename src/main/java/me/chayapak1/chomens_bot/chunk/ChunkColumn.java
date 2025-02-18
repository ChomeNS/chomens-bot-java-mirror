package me.chayapak1.chomens_bot.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.data.chunk.ChunkPos;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;

// Author: hhhzzzsss
public class ChunkColumn {
    public final ChunkPos pos;
    public final ChunkSection[] chunks;

    private final int minY;

    public ChunkColumn (ChunkPos chunkPos, byte[] data, int worldHeight, int minY) {
        this.pos = chunkPos;
        this.minY = minY;

        final ByteBuf in = Unpooled.wrappedBuffer(data);
        final int numSections = -Math.floorDiv(-worldHeight, 16);

        chunks = new ChunkSection[numSections];

        for (int i = 0; i < numSections; i++) {
            chunks[i] = MinecraftTypes.readChunkSection(in);
        }
    }

    public int getBlock (int x, int y, int z) {
        int yIndex = (y - minY) >> 4;

        if (chunks[yIndex] == null) return 0;

        return chunks[yIndex].getBlock(x, y & 15, z);
    }

    public void setBlock (int x, int y, int z, int id) {
        int yIndex = (y - minY) >> 4;

        if (chunks[yIndex] == null) {
            chunks[yIndex] = new ChunkSection();
            chunks[yIndex].setBlock(0, 0, 0, 0);
        }

        chunks[yIndex].setBlock(x, y & 15, z, id);
    }
}
