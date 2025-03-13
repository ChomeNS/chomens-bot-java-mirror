package me.chayapak1.chomens_bot.chunk;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chunk.ChunkPos;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftTypes;
import org.geysermc.mcprotocollib.protocol.data.game.chunk.ChunkSection;

// Author: hhhzzzsss
public class ChunkColumn {
    private final Bot bot;

    public final ChunkPos pos;
    public final ChunkSection[] chunks;

    private final int minY;

    public ChunkColumn (Bot bot, ChunkPos chunkPos, byte[] data, int worldHeight, int minY) {
        this.bot = bot;
        this.pos = chunkPos;
        this.minY = minY;

        final int absoluteWorldHeight = Math.abs(worldHeight);
        final int absoluteMinY = Math.abs(minY);

        final ByteBuf in = Unpooled.wrappedBuffer(data);
        final int numSections = -Math.floorDiv(-(absoluteWorldHeight + absoluteMinY), 16);

        chunks = new ChunkSection[numSections];

        for (int i = 0; i < numSections; i++) {
            chunks[i] = MinecraftTypes.readChunkSection(in);
        }
    }

    public int getBlock (int x, int y, int z) {
        if (chunks == null) return 0;

        int yIndex = (y - minY) >> 4;

        if (yIndex >= chunks.length) return 0;

        return chunks[yIndex].getBlock(x, y & 15, z);
    }

    public void setBlock (int x, int y, int z, int id) {
        int yIndex = (y - minY) >> 4;

        if (yIndex >= chunks.length) return;

        try {
            if (chunks[yIndex] == null) {
                chunks[yIndex] = new ChunkSection();
                chunks[yIndex].setBlock(0, 0, 0, 0);
            }

            chunks[yIndex].setBlock(x, y & 15, z, id);
        } catch (Exception e) {
            // passing bot just for debugging? really?
            bot.logger.error(
                    Component.translatable(
                            "Failed to set block at %s %s %s with state %s!",

                            Component.text(x),
                            Component.text(y),
                            Component.text(z),

                            Component.text(id)
                    )
            );
            bot.logger.error(e);
        }
    }
}
