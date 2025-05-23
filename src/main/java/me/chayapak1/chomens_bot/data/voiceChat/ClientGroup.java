package me.chayapak1.chomens_bot.data.voiceChat;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;

import java.util.Arrays;
import java.util.UUID;

public record ClientGroup(UUID id, String name, boolean hasPassword, boolean persistent, GroupType type) {

    public static ClientGroup fromBytes (final FriendlyByteBuf buf) {
        return new ClientGroup(
                buf.readUUID(),
                buf.readUtf(512),
                buf.readBoolean(),
                buf.readBoolean(),
                GroupType.values()[buf.readShort()]
        );
    }

    public void toBytes (final FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(name, 512);
        buf.writeBoolean(hasPassword);
        buf.writeBoolean(persistent);
        buf.writeShort(Arrays.stream(GroupType.values()).toList().indexOf(type));
    }

}
