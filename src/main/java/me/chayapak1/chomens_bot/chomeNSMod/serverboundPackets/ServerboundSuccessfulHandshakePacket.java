package me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;

public class ServerboundSuccessfulHandshakePacket implements Packet {
    public ServerboundSuccessfulHandshakePacket () {
    }

    public ServerboundSuccessfulHandshakePacket (final ByteBuf buf) {
    }

    @Override
    public int getId () {
        return 0;
    }

    @Override
    public void serialize (final ByteBuf buf) {

    }
}
