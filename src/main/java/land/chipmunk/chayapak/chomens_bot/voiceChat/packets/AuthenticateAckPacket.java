package land.chipmunk.chayapak.chomens_bot.voiceChat.packets;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.Packet;

public class AuthenticateAckPacket implements Packet<AuthenticateAckPacket> {
    @Override
    public AuthenticateAckPacket fromBytes(FriendlyByteBuf buf) {
        return new AuthenticateAckPacket();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
    }
}
