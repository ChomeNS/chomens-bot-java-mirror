package me.chayapak1.chomens_bot.song;

import me.chayapak1.chomens_bot.Bot;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

// Author: hhhzzzsss (i ported it from songplayer)
public class SongPlayerConverter implements Converter {
    public static final byte[] FILE_TYPE_SIGNATURE = {-53, 123, -51, -124, -122, -46, -35, 38};
    public static final long MAX_UNCOMPRESSED_SIZE = 50 * 1024 * 1024;

    @Override
    public Song getSongFromBytes(byte[] bytes, String fileName, Bot bot) throws Exception {
        InputStream is = new LimitedSizeInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)), MAX_UNCOMPRESSED_SIZE);
        bytes = is.readAllBytes();
        is.close();

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for (byte b : FILE_TYPE_SIGNATURE) {
            if (b != buffer.get()) {
                throw new IOException("Invalid file type signature");
            }
        }

        byte version = buffer.get();
        // Currently on format version 1
        if (version != 1) {
            throw new IOException("Unsupported format version!");
        }

        long songLength = buffer.getLong();
        String songName = getString(buffer, bytes.length);
        int loop = buffer.get() & 0xFF;
        int loopCount = buffer.get() & 0xFF;
        long loopPosition = buffer.getLong();

        Song song = new Song(fileName, bot, !songName.trim().isEmpty() ? songName : null, null, null, null, null, false);
        song.length = songLength;
//        song.looping = loop > 0;
//        song.loopCount = loopCount;
        song.loopPosition = loopPosition == 0 ? 200 : loopPosition;

        long time = 0;
        while (true) {
            int noteId = buffer.getShort();
            if (noteId >= 0 && noteId < 400) {
                time += getVarLong(buffer);
                song.add(new Note(Instrument.fromId(noteId / 25), noteId % 25, noteId % 25, 1, time, -1, 100));
            }
            else if ((noteId & 0xFFFF) == 0xFFFF) {
                break;
            }
            else {
                throw new IOException("Song contains invalid note id of " + noteId);
            }
        }

        return song;
    }

    private static String getString(ByteBuffer buffer, int maxSize) throws IOException {
        int length = buffer.getInt();
        if (length > maxSize) {
            throw new IOException("String is too large");
        }
        byte[] arr = new byte[length];
        buffer.get(arr, 0, length);
        return new String(arr, StandardCharsets.UTF_8);
    }

    private static long getVarLong(ByteBuffer buffer) {
        long val = 0;
        long mult = 1;
        int flag = 1;
        while (flag != 0) {
            int b = buffer.get() & 0xFF;
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

        public LimitedSizeInputStream(InputStream original, long maxSize) {
            this.original = original;
            this.maxSize = maxSize;
        }

        @Override
        public int read() throws IOException {
            int i = original.read();
            if (i>=0) incrementCounter(1);
            return i;
        }

        @Override
        public int read(byte b[]) throws IOException {
            return read(b, 0, b.length);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            int i = original.read(b, off, len);
            if (i>=0) incrementCounter(i);
            return i;
        }

        private void incrementCounter(int size) throws IOException {
            total += size;
            if (total>maxSize) throw new IOException("Input stream exceeded maximum size of " + maxSize + " bytes");
        }
    }
}
