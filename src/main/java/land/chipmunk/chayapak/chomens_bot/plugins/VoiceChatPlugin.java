package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import io.netty.buffer.Unpooled;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.AES;
import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;

// exists for some reason, still wip and will be finished in the next 69 years
// and i prob implemented it in a wrong way lol
// at least when you do `/voicechat test (bot username)` it will show `Client not connected`
// instead of `(bot username) does not have Simple Voice Chat installed`
public class VoiceChatPlugin extends Bot.Listener {
    public static final byte MAGIC_BYTE = (byte) 0b11111111;

    private final Bot bot;

    private final ClientVoiceChatSocket socket;

    public VoiceChatPlugin(Bot bot) {
        this.bot = bot;
        this.socket = new ClientVoiceChatSocket();
        try {
            socket.open();
        } catch (Exception e) {
            e.printStackTrace();
        }

        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
        else if (packet instanceof ClientboundCustomPayloadPacket) packetReceived((ClientboundCustomPayloadPacket) packet);
    }

    public void packetReceived(ClientboundLoginPacket ignored) {
        // totally didn't use a real minecraft client with voicechat mod to get this
        bot.session().send(new ServerboundCustomPayloadPacket(
                "minecraft:brand",
                "\u0006fabric".getBytes() // should i use fabric here?
        ));

        bot.session().send(new ServerboundCustomPayloadPacket(
                "voicechat:request_secret",
                new byte[] { 0, 0, 0, 17 } // what are these bytes?
        ));
    }

    public void packetReceived(ClientboundCustomPayloadPacket _packet) {
        // sus
        /*
        System.out.println(_packet.getChannel());
        System.out.println(Arrays.toString(_packet.getData()));
        System.out.println(new String(_packet.getData()));
        */
        // for some reason this entire part is not running
        if (_packet.getChannel().equals("voicechat:secret")) {
            bot.executorService().submit(() -> {
                while (true) {
                    final RawUdpPacket packet = socket.read();

                    if (packet == null) return;

                    byte[] data = packet.data();
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

                    if (buf.readByte() != MAGIC_BYTE) return;

                    // AES.decrypt(secret, payload) ?
                }
            });
        }
    }

    private static class VoiceChatSocketBase {
        private final byte[] BUFFER = new byte[4096];

        public RawUdpPacket read (DatagramSocket socket) {
            try {
                DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);
                socket.receive(packet);
                // Setting the timestamp after receiving the packet
                long timestamp = System.currentTimeMillis();
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                return new RawUdpPacket(data, packet.getSocketAddress(), timestamp);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

    }

    private static class ClientVoiceChatSocket extends VoiceChatSocketBase {
        private DatagramSocket socket;

        public void open() throws SocketException {
            this.socket = new DatagramSocket();
        }

        public RawUdpPacket read() {
            if (socket == null) {
                throw new IllegalStateException("Socket not opened yet");
            }
            return read(socket);
        }

        public void send(byte[] data, SocketAddress address) throws Exception {
            if (socket == null) {
                return; // Ignoring packet sending when socket isn't open yet
            }
            socket.send(new DatagramPacket(data, data.length, address));
        }

        public void close() {
            if (socket != null) {
                socket.close();
            }
        }

        public boolean isClosed() {
            return socket == null;
        }
    }

    @AllArgsConstructor
    private static class RawUdpPacket {
        @Getter private final byte[] data;
        @Getter private final SocketAddress socketAddress;
        @Getter private final long timestamp;
    }
}
