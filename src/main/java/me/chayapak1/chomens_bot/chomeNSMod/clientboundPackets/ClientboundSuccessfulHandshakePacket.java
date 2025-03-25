package me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;

public class ClientboundSuccessfulHandshakePacket implements Packet {
    public ClientboundSuccessfulHandshakePacket () {
    }

    public ClientboundSuccessfulHandshakePacket (ByteBuf buf) {
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public void serialize (ByteBuf buf) {

    }
}
