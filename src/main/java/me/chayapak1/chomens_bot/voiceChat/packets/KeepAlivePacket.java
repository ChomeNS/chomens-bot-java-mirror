package me.chayapak1.chomens_bot.voiceChat.packets;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.Packet;

public class KeepAlivePacket implements Packet<KeepAlivePacket> {
    @Override
    public KeepAlivePacket fromBytes (FriendlyByteBuf buf) {
        return new KeepAlivePacket();
    }

    @Override
    public void toBytes (FriendlyByteBuf buf) {
    }
}
