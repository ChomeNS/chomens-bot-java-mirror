package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;
import lombok.Getter;

import java.util.UUID;

public class PingPacket implements Packet<PingPacket> {
    @Getter private UUID id;
    @Getter private long timestamp;

    @Override
    public PingPacket fromBytes(FriendlyByteBuf buf) {
        final PingPacket pingPacket = new PingPacket();
        pingPacket.id = buf.readUUID();
        pingPacket.timestamp = buf.readLong();
        return pingPacket;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeLong(timestamp);
    }
}
