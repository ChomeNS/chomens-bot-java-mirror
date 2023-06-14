package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import io.netty.buffer.Unpooled;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.voiceChat.RawUdpPacket;
import land.chipmunk.chayapak.chomens_bot.util.FriendlyByteBuf;
import land.chipmunk.chayapak.chomens_bot.voiceChat.InitializationData;
import land.chipmunk.chayapak.chomens_bot.voiceChat.NetworkMessage;
import land.chipmunk.chayapak.chomens_bot.voiceChat.packets.KeepAlivePacket;
import land.chipmunk.chayapak.chomens_bot.voiceChat.packets.PingPacket;
import land.chipmunk.chayapak.chomens_bot.voiceChat.packets.SecretPacket;
import land.chipmunk.chayapak.chomens_bot.voiceChat.packets.AuthenticatePacket;

import java.net.*;

// exists for some reason, still wip and will be finished in the next 69 years
// and i prob implemented it in a wrong way lol
// at least when you do `/voicechat test (bot username)` it will show `Client not connected`
// instead of `(bot username) does not have Simple Voice Chat installed`
public class VoiceChatPlugin extends Bot.Listener {
    private final Bot bot;

    private InitializationData initializationData;
    private ClientVoiceChatSocket socket;
    private InetAddress address;
    private InetSocketAddress socketAddress;

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
    }

    public void packetReceived(ClientboundCustomPayloadPacket _packet) {
        // sus
        /*
        System.out.println("\"" + _packet.getChannel() + "\"");
        System.out.println(Arrays.toString(_packet.getData()));
        System.out.println(new String(_packet.getData()));
        */
        if (_packet.getChannel().equals("voicechat:secret")) {
            System.out.println("got the secret packet");

            final byte[] bytes = _packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final SecretPacket secretPacket = new SecretPacket().fromBytes(buf);

            System.out.println(secretPacket.secret());
            System.out.println(secretPacket.serverPort());

            initializationData = new InitializationData(bot.options().host(), secretPacket);

            try {
                address = InetAddress.getByName(bot.options().host());
                socketAddress = new InetSocketAddress(address, initializationData.serverPort());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            System.out.println("address " + address.getHostName());

            socket = new ClientVoiceChatSocket();
            try {
                socket.open();
            } catch (Exception e) {
                e.printStackTrace();
            }

            bot.executorService().submit(() -> {
                while (true) {
                    try {
                        if (socket.isClosed()) continue;

                        System.out.println("reading packet");
                        final NetworkMessage message = NetworkMessage.readPacket(socket.read(), initializationData);
                        System.out.println("DONE reading packet, this message will; prob nto woshow");

                        if (message == null) continue;

                        if (message.packet() instanceof AuthenticatePacket) {
                            System.out.println("SERVER AUTHENTICATED FINALLYYYYYYYYYYYYYYYY");
                        } else if (message.packet() instanceof PingPacket pingPacket) {
                            System.out.println("got ping packet");
                            sendToServer(new NetworkMessage(pingPacket));
                        } else if (message.packet() instanceof KeepAlivePacket) {
                            System.out.println("got keep alive packet");
                            sendToServer(new NetworkMessage(new KeepAlivePacket()));
                        } else {
                            System.out.println("got " + message.packet().getClass().getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public void sendToServer (NetworkMessage message) throws Exception {
        socket.send(
                message.writeClient(initializationData),
                socketAddress
        );
    }

    private static class VoiceChatSocketBase {
        private final byte[] BUFFER = new byte[4096];

        public RawUdpPacket read (DatagramSocket socket) {
            try {
                DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);

                System.out.println("receiving packet");

                // the problem is this next line, just this 1 line. it makes the entire thing froze
                socket.receive(packet);

                System.out.println("FINALLY DONE RECEIVING AAAAHHHHHHHHHHHHHHHHHHH");

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
}
