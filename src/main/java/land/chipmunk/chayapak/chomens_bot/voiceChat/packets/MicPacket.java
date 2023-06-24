package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class MicPacket implements Packet<MicPacket> {
    @Getter private byte[] data;
    @Getter private boolean whispering;
    @Getter private long sequenceNumber;

    public MicPacket() {}

    @Override
    public MicPacket fromBytes(FriendlyByteBuf buf) {
        MicPacket soundPacket = new MicPacket();
        soundPacket.data = buf.readByteArray();
        soundPacket.sequenceNumber = buf.readLong();
        soundPacket.whispering = buf.readBoolean();
        return soundPacket;
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeByteArray(data);
        buf.writeLong(sequenceNumber);
        buf.writeBoolean(whispering);
    }
}
