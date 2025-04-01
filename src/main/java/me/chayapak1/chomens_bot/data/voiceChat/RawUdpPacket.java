package me.chayapak1.chomens_bot.data.voiceChat;

import java.net.SocketAddress;

public record RawUdpPacket(byte[] data, SocketAddress socketAddress, long timestamp) { }
