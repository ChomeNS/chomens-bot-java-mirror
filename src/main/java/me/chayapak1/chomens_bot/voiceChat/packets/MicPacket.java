package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

public class MicPacket implements Packet<MicPacket> {
    public byte[] data;
    public boolean whispering;
    public long sequenceNumber;

    public MicPacket () { }

    public MicPacket (
            byte[] data,
            boolean whispering,
            long sequenceNumber
    ) {
        this.data = data;
        this.whispering = whispering;
        this.sequenceNumber = sequenceNumber;
    }

    @Override
    public MicPacket fromBytes (FriendlyByteBuf buf) {
        MicPacket soundPacket = new MicPacket();
        soundPacket.data = buf.readByteArray();
        soundPacket.sequenceNumber = buf.readLong();
        soundPacket.whispering = buf.readBoolean();
        return soundPacket;
    }

    @Override
    public void toBytes (FriendlyByteBuf buf) {
        buf.writeByteArray(data);
        buf.writeLong(sequenceNumber);
        buf.writeBoolean(whispering);
    }
}
