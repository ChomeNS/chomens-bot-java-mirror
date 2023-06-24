package land.chipmunk.chayapak.chomens_bot.voiceChat.customPayload;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
public class JoinGroupPacket implements Packet<JoinGroupPacket> {
    @Getter private UUID group;
    @Getter private String password;

    public JoinGroupPacket () {}

    @Override
    public JoinGroupPacket fromBytes(FriendlyByteBuf buf) {
        group = buf.readUUID();
        if (buf.readBoolean()) {
            password = buf.readUtf(512);
        }
        return this;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(group);
        buf.writeBoolean(password != null);
        if (password != null) {
            buf.writeUtf(password, 512);
        }
    }
}
