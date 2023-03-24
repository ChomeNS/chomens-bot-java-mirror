package me.chayapak1.chomens_bot.util;

import com.github.steveice10.opennbt.tag.builtin.IntArrayTag;
import java.util.UUID;
import java.nio.ByteBuffer;

public class UUIDUtilities {
    private UUIDUtilities () {}

    public static int[] intArray (UUID uuid) {
        final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(0, uuid.getMostSignificantBits());
        buffer.putLong(8, uuid.getLeastSignificantBits());

        final int[] intArray = new int[4];
        for (int i = 0; i < intArray.length; i++) intArray[i] = buffer.getInt();

        return intArray;
    }

    public static IntArrayTag tag (UUID uuid) {
        return new IntArrayTag("", intArray(uuid));
    }

    public static String snbt (UUID uuid) {
        int[] array = intArray(uuid);
        return "[I;" + array[0] + "," + array[1] + "," + array[2] + "," + array[3] + "]"; // TODO: improve lol
    }

    public static String selector (UUID uuid) { return "@a[limit=1,nbt={UUID:" + snbt(uuid) + "}]"; }
    public static String exclusiveSelector (UUID uuid) { return "@a[nbt=!{UUID:" + snbt(uuid) + "}]"; }
}
