package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import io.netty.buffer.Unpooled;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.voiceChat.RawUdpPacket;
import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.InitializationData;
import land.chipmunk.chayapak.chomens_bot.voiceChat.NetworkMessage;
import land.chipmunk.chayapak.chomens_bot.voiceChat.packets.*;

import java.net.*;

// most of these codes are from the simple voice chat mod itself including the other voicechat classes
// i didn't implement mic yet because my goal is to make `/voicechat test` work with the bot
public class VoiceChatPlugin extends Bot.Listener {
    private final Bot bot;

    private InitializationData initializationData;
    private ClientVoiceChatSocket socket;
    private InetSocketAddress socketAddress;

    private boolean running = false;

    public VoiceChatPlugin(Bot bot) {
        this.bot = bot;

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
                new FriendlyByteBuf(Unpooled.buffer()).writeInt(17).array()
        ));

        bot.session().send(new ServerboundCustomPayloadPacket(
                "voicechat:update_state",
                new FriendlyByteBuf(Unpooled.buffer()).writeBoolean(false).array()
        ));

        running = true;
    }

    public void packetReceived(ClientboundCustomPayloadPacket _packet) {
        if (_packet.getChannel().equals("voicechat:secret")) { // fard
            final byte[] bytes = _packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final SecretPacket secretPacket = new SecretPacket().fromBytes(buf);

            initializationData = new InitializationData(bot.options().host(), secretPacket);

            try {
                final InetAddress address = InetAddress.getByName(bot.options().host());
                socketAddress = new InetSocketAddress(address, initializationData.serverPort());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            socket = new ClientVoiceChatSocket();
            try {
                socket.open();
            } catch (Exception e) {
                e.printStackTrace();
            }

            new Thread(() -> {
                sendToServer(new NetworkMessage(new AuthenticatePacket(initializationData.playerUUID(), initializationData.secret())));

                while (running) {
                    try {
                        final NetworkMessage message = NetworkMessage.readPacket(socket.read(), initializationData);

                        if (message == null) continue;

                        if (message.packet() instanceof PingPacket pingPacket) sendToServer(new NetworkMessage(pingPacket));
                        else if (message.packet() instanceof KeepAlivePacket) sendToServer(new NetworkMessage(new KeepAlivePacket()));
                        else if (message.packet() instanceof AuthenticateAckPacket) sendToServer(new NetworkMessage(new ConnectionCheckPacket()));
                    } catch (Exception e) {
                        if (running) e.printStackTrace();
                        else break; // is this neccessary?
                    }
                }
            }).start();
        }
    }

    public void sendToServer (NetworkMessage message) {
        try {
            socket.send(
                    message.writeClient(initializationData),
                    socketAddress
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class VoiceChatSocketBase {
        private final byte[] BUFFER = new byte[4096];

        public RawUdpPacket read (DatagramSocket socket) {
            if (socket.isClosed()) return null;
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

    @Override
    public void disconnected(DisconnectedEvent event) {
        socket.close();

        running = false;
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
                socket = null;
            }
        }
    }
}
