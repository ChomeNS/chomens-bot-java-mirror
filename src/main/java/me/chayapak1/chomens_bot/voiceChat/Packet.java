package me.chayapak1.chomens_bot.voiceChat;

import me.chayapak1.chomens_bot.util.FriendlyByteBuf;

public interface Packet<T extends Packet<T>> {
    T fromBytes (FriendlyByteBuf buf);

    void toBytes (FriendlyByteBuf buf);
}
