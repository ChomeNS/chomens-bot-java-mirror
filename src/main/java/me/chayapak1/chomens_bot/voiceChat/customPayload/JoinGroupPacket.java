package me.chayapak1.chomens_bot.voiceChat.customPayload;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

import java.util.UUID;

public class JoinGroupPacket implements Packet<JoinGroupPacket> {
    public UUID group;
    public String password;

    public JoinGroupPacket () { }

    public JoinGroupPacket (final UUID group, final String password) {
        this.group = group;
        this.password = password;
    }

    @Override
    public JoinGroupPacket fromBytes (final FriendlyByteBuf buf) {
        group = buf.readUUID();
        if (buf.readBoolean()) {
            password = buf.readUtf(512);
        }
        return this;
    }

    @Override
    public void toBytes (final FriendlyByteBuf buf) {
        buf.writeUUID(group);
        buf.writeBoolean(password != null);
        if (password != null) {
            buf.writeUtf(password, 512);
        }
    }
}
