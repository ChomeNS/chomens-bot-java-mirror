package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;

public class ConnectionAckPacket implements Packet<ConnectionAckPacket> {
    @Override
    public ConnectionAckPacket fromBytes(FriendlyByteBuf buf) {
        return new ConnectionAckPacket();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
    }
}
