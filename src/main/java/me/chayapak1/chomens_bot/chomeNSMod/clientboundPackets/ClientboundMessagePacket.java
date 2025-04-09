package me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.Types;
import net.kyori.adventure.text.Component;

public class ClientboundMessagePacket implements Packet {
    public final Component message;

    public ClientboundMessagePacket (Component message) {
        this.message = message;
    }

    public ClientboundMessagePacket (ByteBuf buf) {
        this.message = Types.readComponent(buf);
    }

    @Override
    public int getId () {
        return 2;
    }

    @Override
    public void serialize (ByteBuf buf) {
        Types.writeComponent(buf, this.message);
    }
}
