package me.chayapak1.chomens_bot.plugins;

import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.voiceChat.ClientGroup;
import me.chayapak1.chomens_bot.data.voiceChat.RawUdpPacket;
import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.voiceChat.InitializationData;
import me.chayapak1.chomens_bot.voiceChat.NetworkMessage;
import me.chayapak1.chomens_bot.voiceChat.customPayload.JoinGroupPacket;
import me.chayapak1.chomens_bot.voiceChat.customPayload.SecretPacket;
import me.chayapak1.chomens_bot.voiceChat.packets.*;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ALMOST ALL of these codes are from the simple voice chat mod itself including the other voicechat classes
// mic packet exists but is never sent because i am too lazy to implement the player + evilbot already has a voicechat music player
public class VoiceChatPlugin extends Bot.Listener {
    private final Bot bot;

    private InitializationData initializationData;
    private ClientVoiceChatSocket socket;
    private InetSocketAddress socketAddress;

    private boolean running = false;

    public final List<ClientGroup> groups = new ArrayList<>();

    public VoiceChatPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundCustomPayloadPacket t_packet) packetReceived(t_packet);
    }

    public void packetReceived(ClientboundLoginPacket ignored) {
        // totally didn't use a real minecraft client with voicechat mod to get this

        bot.session.send(new ServerboundCustomPayloadPacket(
                Key.key("voicechat:request_secret"),
                new FriendlyByteBuf(Unpooled.buffer()).writeInt(18).array()
        ));

        bot.session.send(new ServerboundCustomPayloadPacket(
                Key.key("voicechat:update_state"),
                new FriendlyByteBuf(Unpooled.buffer()).writeBoolean(false).array()
        ));

        running = true;
    }

    public void packetReceived(ClientboundCustomPayloadPacket _packet) {
        if (_packet.getChannel().equals(Key.key("voicechat:secret"))) { // fard
            final byte[] bytes = _packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            final SecretPacket secretPacket = new SecretPacket().fromBytes(buf);
            initializationData = new InitializationData(secretPacket);

            final InetSocketAddress mcAddress = (InetSocketAddress) bot.session.getRemoteAddress();
            socketAddress = new InetSocketAddress(mcAddress.getAddress(), initializationData.serverPort);

            socket = new ClientVoiceChatSocket();
            try {
                socket.open();
            } catch (Exception e) {
                bot.logger.error(e);
            }

            final Thread thread = new Thread(() -> {
                sendToServer(new NetworkMessage(new AuthenticatePacket(initializationData.playerUUID, initializationData.secret)));

                while (running) {
                    try {
                        final NetworkMessage message = NetworkMessage.readPacket(socket.read(), initializationData);

                        if (message == null) continue;

                        if (message.packet instanceof PingPacket pingPacket) sendToServer(new NetworkMessage(pingPacket));
                        else if (message.packet instanceof KeepAlivePacket) sendToServer(new NetworkMessage(new KeepAlivePacket()));
                        else if (message.packet instanceof AuthenticateAckPacket) sendToServer(new NetworkMessage(new ConnectionCheckPacket()));
                    } catch (Exception e) {
                        if (running) bot.logger.error(e);
                        else break; // is this neccessary?
                    }
                }
            });

            thread.setName("Simple Voice Chat Thread");
            thread.start();
        } else if (_packet.getChannel().equals(Key.key("voicechat:add_group"))) {
            final byte[] bytes = _packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final ClientGroup group = ClientGroup.fromBytes(buf);

            groups.add(group);
        } else if (_packet.getChannel().equals(Key.key("voicechat:remove_group"))) {
            final byte[] bytes = _packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final UUID id = buf.readUUID();

            groups.removeIf((group) -> group.id().equals(id));
        }
    }

    public void joinGroup (String group, String password) {
        final ClientGroup[] clientGroups = groups
                .stream()
                .filter(eachGroup -> eachGroup.name().equals(group))
                .toArray(ClientGroup[]::new);

        if (clientGroups.length == 0) throw new RuntimeException("Group " + group + " doesn't exist");

        final ClientGroup clientGroup = clientGroups[0];

        final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        new JoinGroupPacket(clientGroup.id(), password).toBytes(buf);

        bot.session.send(new ServerboundCustomPayloadPacket(
                Key.key("voicechat:set_group"),
                buf.array()
        ));
    }

    public void sendToServer (NetworkMessage message) {
        try {
            socket.send(
                    message.writeClient(initializationData),
                    socketAddress
            );
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    private class VoiceChatSocketBase {
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
                if (!running) return null;

                bot.logger.error(e);
            }

            return null;
        }

    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        socket.close();

        groups.clear();

        running = false;
    }

    private class ClientVoiceChatSocket extends VoiceChatSocketBase {
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
