package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

public class ConnectionAckPacket implements Packet<ConnectionAckPacket> {
    @Override
    public ConnectionAckPacket fromBytes (final FriendlyByteBuf buf) {
        return new ConnectionAckPacket();
    }

    @Override
    public void toBytes (final FriendlyByteBuf buf) {
    }
}
