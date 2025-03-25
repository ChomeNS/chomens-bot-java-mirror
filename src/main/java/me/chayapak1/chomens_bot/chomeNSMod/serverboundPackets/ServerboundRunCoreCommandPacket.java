package me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets;

import io.netty.buffer.ByteBuf;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.Types;

import java.util.UUID;

public class ServerboundRunCoreCommandPacket implements Packet {
    public final UUID runID;
    public final String command;

    public ServerboundRunCoreCommandPacket (UUID runID, String command) {
        this.runID = runID;
        this.command = command;
    }

    public ServerboundRunCoreCommandPacket (ByteBuf buf) {
        this.runID = Types.readUUID(buf);
        this.command = Types.readString(buf);
    }

    @Override
    public int getId () {
        return 1;
    }

    @Override
    public void serialize (ByteBuf buf) {
        Types.writeUUID(buf, this.runID);
        Types.writeString(buf, this.command);
    }
}
