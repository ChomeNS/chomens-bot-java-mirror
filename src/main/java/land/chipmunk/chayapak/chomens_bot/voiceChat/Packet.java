package land.chipmunk.chayapak.chomens_bot.voiceChat;

import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;

public interface Packet<T extends Packet<T>> {
    T fromBytes (FriendlyByteBuf buf);

    void toBytes (FriendlyByteBuf buf);
}
