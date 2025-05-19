package me.chayapak1.chomens_bot.chomeNSMod;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.util.SNBTUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Types {
    public static UUID readUUID (final ByteBuf buf) {
        final long mostSignificantBits = buf.readLong();
        final long leastSignificantBits = buf.readLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static void writeUUID (final ByteBuf buf, final UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writeString (final ByteBuf buf, final String string) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString (final ByteBuf buf) {
        final int length = buf.readInt();

        final byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Component readComponent (final ByteBuf buf) {
        final String stringJSON = readString(buf);

        try {
            return GsonComponentSerializer.gson().deserialize(stringJSON);
        } catch (final Exception e) {
            return null;
        }
    }

    public static void writeComponent (final ByteBuf buf, final Component component) {
        final String stringJSON = SNBTUtilities.fromComponent(false, component);

        writeString(buf, stringJSON);
    }
}
