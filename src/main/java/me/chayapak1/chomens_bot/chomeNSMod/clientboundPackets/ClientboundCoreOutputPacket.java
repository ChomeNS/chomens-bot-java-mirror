package me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.Types;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class ClientboundCoreOutputPacket implements Packet {
    public final UUID runID;
    public final Component output;

    public ClientboundCoreOutputPacket (final UUID runID, final Component output) {
        this.runID = runID;
        this.output = output;
    }

    public ClientboundCoreOutputPacket (final ByteBuf buf) {
        this.runID = Types.readUUID(buf);
        this.output = Types.readComponent(buf);
    }

    @Override
    public int getId () {
        return 1;
    }

    @Override
    public void serialize (final ByteBuf buf) {
        Types.writeUUID(buf, this.runID);
        Types.writeComponent(buf, this.output);
    }
}
