package land.chipmunk.chayapak.chomens_bot.data.voiceChat;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
public class ClientGroup {
    @Getter private final UUID id;
    @Getter private final String name;
    @Getter private final boolean hasPassword;
    @Getter private final boolean persistent;
    @Getter private final GroupType type;

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
