package me.chayapak1.chomens_bot.util;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;

import java.nio.ByteBuffer;
import java.util.UUID;

// Author: _ChipMC_ for int array and selector stuff
public class UUIDUtilities {
    public static UUID getOfflineUUID (final String username) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes());
    }

    public static UUID tryParse (final String input) {
        try {
            return UUID.fromString(input);
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    public static int[] intArray (final UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(0, uuid.getMostSignificantBits());
        buffer.putLong(8, uuid.getLeastSignificantBits());

        final int[] intArray = new int[4];
        for (int i = 0; i < intArray.length; i++) intArray[i] = buffer.getInt();

        return intArray;
    }

    public static NbtMap tag (final UUID uuid) {
        final NbtMapBuilder builder = NbtMap.builder();

        builder.putIntArray("", intArray(uuid));

        return builder.build();
    }

    public static String snbt (final UUID uuid) {
        final int[] array = intArray(uuid);
        return String.format(
                "[I;%d,%d,%d,%d]",
                array[0],
                array[1],
                array[2],
                array[3]
        );
    }

    public static String selector (final UUID uuid) { return selector(uuid, true); }

    public static String selector (final UUID uuid, final boolean end) { return "@p[nbt={UUID:" + snbt(uuid) + "}" + (end ? "]" : ""); }

    public static String exclusiveSelector (final UUID uuid) { return exclusiveSelector(uuid, true); }

    public static String exclusiveSelector (final UUID uuid, final boolean end) { return "@a[nbt=!{UUID:" + snbt(uuid) + "}" + (end ? "]" : ""); }
}
