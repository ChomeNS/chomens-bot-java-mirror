package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

public class ConnectionCheckPacket implements Packet<ConnectionCheckPacket> {
    @Override
    public ConnectionCheckPacket fromBytes(FriendlyByteBuf buf) {
        return new ConnectionCheckPacket();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
    }
}
