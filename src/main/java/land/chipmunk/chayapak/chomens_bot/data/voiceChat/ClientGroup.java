package land.chipmunk.chayapak.chomens_bot.data.voiceChat;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;

import java.util.UUID;

public record ClientGroup(UUID id, String name, boolean hasPassword, boolean persistent, GroupType type) {
    public static ClientGroup fromBytes(FriendlyByteBuf buf) {
        return new ClientGroup(
                buf.readUUID(),
                buf.readUtf(512),
                buf.readBoolean(),
                buf.readBoolean(),
                GroupType.values()[buf.readShort()]
        );
    }
}
