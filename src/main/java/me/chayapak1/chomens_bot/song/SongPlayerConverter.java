package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;
import org.cloudburstmc.math.vector.Vector3d;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

// Author: hhhzzzsss (i ported it from songplayer)
public class SongPlayerConverter implements Converter {
    public static final byte[] FILE_TYPE_SIGNATURE = { -53, 123, -51, -124, -122, -46, -35, 38 };
    public static final long MAX_UNCOMPRESSED_SIZE = 50 * 1024 * 1024;

    @Override
    public Song getSongFromBytes (byte[] bytes, final String fileName, final Bot bot) throws Exception {
        final InputStream is = new LimitedSizeInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)), MAX_UNCOMPRESSED_SIZE);
        bytes = is.readAllBytes();
        is.close();

        final ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (final byte b : FILE_TYPE_SIGNATURE) {
            if (b != buffer.get()) {
                throw new IOException("Invalid file type signature");
            }
        }

        final byte version = buffer.get();
        // Currently on format version 1
        if (version != 1) {
            throw new IOException("Unsupported format version!");
        }

        final long songLength = buffer.getLong();
        final String songName = getString(buffer, bytes.length);
        final int loop = buffer.get() & 0xFF;
        final int loopCount = buffer.get() & 0xFF;
        final long loopPosition = buffer.getLong();

        final Song song = new Song(fileName, bot, !songName.trim().isEmpty() ? songName : null, null, null, null, null, false);
        song.length = songLength;
        //        song.looping = loop > 0;
        //        song.loopCount = loopCount;
        song.loopPosition = loopPosition;

        long time = 0;
        while (true) {
            final int noteId = buffer.getShort();
            if (noteId >= 0 && noteId < 400) {
                time += getVarLong(buffer);
                song.add(
                        new Note(
                                Instrument.fromId(noteId / 25),
                                noteId % 25,
                                noteId % 25,
                                1,
                                time,
                                Vector3d.ZERO,
                                false
                        )
                );
            } else if ((noteId & 0xFFFF) == 0xFFFF) {
                break;
            } else {
                throw new IOException("Song contains invalid note id of " + noteId);
            }
        }

        return song;
    }

    private static String getString (final ByteBuffer buffer, final int maxSize) throws IOException {
        final int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        final byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr, StandardCharsets.UTF_8);
    }

    private static long getVarLong (final ByteBuffer buffer) {
        long val = 0;
        long mult = 1;
        int flag = 1;
        while (flag != 0) {
            final int b = buffer.get() & 0xFF;
            val += (b & 0x7F) * mult;
            mult <<= 7;
            flag = b >>> 7;
        }
        return val;
    }

    private static class LimitedSizeInputStream extends InputStream {
        private final InputStream original;
        private final long maxSize;
        private long total;

        public LimitedSizeInputStream (final InputStream original, final long maxSize) {
            this.original = original;
            this.maxSize = maxSize;
        }

        @Override
        public int read () throws IOException {
            final int i = original.read();
            if (i >= 0) incrementCounter(1);
            return i;
        }

        @Override
        public int read (final byte @NotNull [] b) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read (final byte @NotNull [] b, final int off, final int len) throws IOException {
            final int i = original.read(b, off, len);
            if (i >= 0) incrementCounter(i);
            return i;
        }

        private void incrementCounter (final int size) throws IOException {
            total += size;
            if (total > maxSize) throw new IOException("Input stream exceeded maximum size of " + maxSize + " bytes");
        }
    }
}
