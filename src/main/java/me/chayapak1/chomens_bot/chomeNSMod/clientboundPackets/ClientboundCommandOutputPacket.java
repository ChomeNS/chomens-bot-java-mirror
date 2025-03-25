package me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.Types;
import net.kyori.adventure.text.Component;

public class ClientboundCommandOutputPacket implements Packet {
    public final Component output;

    public ClientboundCommandOutputPacket (Component output) {
        this.output = output;
    }

    public ClientboundCommandOutputPacket (ByteBuf buf) {
        this.output = Types.readComponent(buf);
    }

    @Override
    public int getId () {
        return 2;
    }

    @Override
    public void serialize (ByteBuf buf) {
        Types.writeComponent(buf, this.output);
    }
}
