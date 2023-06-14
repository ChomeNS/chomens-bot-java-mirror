package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;

public class KeepAlivePacket implements Packet<KeepAlivePacket> {
    @Override
    public KeepAlivePacket fromBytes(FriendlyByteBuf buf) {
        return new KeepAlivePacket();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
    }
}
