package me.chayapak1.chomens_bot.chomeNSMod;

import io.netty.buffer.ByteBuf;

public interface Packet {
    int getId ();

    void serialize (ByteBuf buf);
}
