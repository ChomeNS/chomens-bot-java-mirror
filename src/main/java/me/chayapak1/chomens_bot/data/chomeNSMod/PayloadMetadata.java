package me.chayapak1.chomens_bot.data.chomeNSMod;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;

public record PayloadMetadata(byte[] nonce, long timestamp) {
    public static PayloadMetadata deserialize (ByteBuf buf) {
        final byte[] nonce = new byte[8];

        buf.readBytes(nonce);

        final long timestamp = buf.readLong();

        return new PayloadMetadata(nonce, timestamp);
    }

    public void serialize (ByteBuf buf) {
        buf.writeBytes(nonce);
        buf.writeLong(timestamp);
    }

    @Override
    public boolean equals (final Object object) {
        if (object == null || getClass() != object.getClass()) return false;

        final PayloadMetadata metadata = (PayloadMetadata) object;

        // java is so fucky about byte[]......... i have to use Arrays.equals()
        return timestamp == metadata.timestamp && Arrays.equals(nonce, metadata.nonce);
    }

    @Override
    public String toString () {
        return "PayloadMetadata{" +
                "nonce=" + Arrays.toString(nonce) +
                ", timestamp=" + timestamp +
                '}';
    }
}
