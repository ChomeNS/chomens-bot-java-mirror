package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

public class AuthenticateAckPacket implements Packet<AuthenticateAckPacket> {
    @Override
    public AuthenticateAckPacket fromBytes(FriendlyByteBuf buf) {
        return new AuthenticateAckPacket();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
    }
}
