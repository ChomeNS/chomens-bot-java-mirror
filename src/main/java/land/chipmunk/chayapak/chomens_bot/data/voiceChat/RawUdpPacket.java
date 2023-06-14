package land.chipmunk.chayapak.chomens_bot.data.voiceChat;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.SocketAddress;

@AllArgsConstructor
public class RawUdpPacket {
    @Getter private final byte[] data;
    @Getter private final SocketAddress socketAddress;
    @Getter private final long timestamp;
}
