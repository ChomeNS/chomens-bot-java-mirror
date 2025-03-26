package me.chayapak1.chomens_bot.chomeNSMod;

import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Types {
    public static UUID readUUID (ByteBuf buf) {
        final long mostSignificantBits = buf.readLong();
        final long leastSignificantBits = buf.readLong();

        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    public static void writeUUID (ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static void writeString (ByteBuf buf, String string) {
        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString (ByteBuf buf) {
        final int length = buf.readInt();

        final byte[] bytes = new byte[length];
        buf.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static Component readComponent (ByteBuf buf) {
        final String stringJSON = readString(buf);

        try {
            return GsonComponentSerializer.gson().deserialize(stringJSON);
        } catch (Exception e) {
            return null;
        }
    }

    public static void writeComponent (ByteBuf buf, Component component) {
        final String stringJSON = GsonComponentSerializer.gson().serialize(component);

        writeString(buf, stringJSON);
    }
}
