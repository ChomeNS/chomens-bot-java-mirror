package me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.Types;

public class ServerboundRunCommandPacket implements Packet {
    public final String input;

    public ServerboundRunCommandPacket (String input) {
        this.input = input;
    }

    public ServerboundRunCommandPacket (ByteBuf buf) {
        this.input = Types.readString(buf);
    }

    @Override
    public int getId () {
        return 2;
    }

    @Override
    public void serialize (ByteBuf buf) {
        Types.writeString(buf, this.input);
    }
}
