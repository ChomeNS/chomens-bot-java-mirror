package me.chayapak1.chomens_bot.plugins;

import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.voiceChat.ClientGroup;
import me.chayapak1.chomens_bot.data.voiceChat.RawUdpPacket;
import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.voiceChat.InitializationData;
import me.chayapak1.chomens_bot.voiceChat.NetworkMessage;
import me.chayapak1.chomens_bot.voiceChat.customPayload.JoinGroupPacket;
import me.chayapak1.chomens_bot.voiceChat.customPayload.SecretPacket;
import me.chayapak1.chomens_bot.voiceChat.packets.*;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// ALMOST ALL of these codes are from the simple voice chat mod itself including the other voicechat classes
// mic packet exists but is never sent because i am too lazy to implement the player + evilbot already has a voicechat music player
public class VoiceChatPlugin implements Listener, Runnable {
    private static final Key SECRET_KEY = Key.key("voicechat:secret");
    private static final Key ADD_GROUP_KEY = Key.key("voicechat:add_group");
    private static final Key REMOVE_GROUP_KEY = Key.key("voicechat:remove_group");

    private final Bot bot;

    private InitializationData initializationData;
    private ClientVoiceChatSocket socket;
    private InetSocketAddress socketAddress;

    private boolean running = false;

    public final List<ClientGroup> groups = new ArrayList<>();

    public VoiceChatPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundCustomPayloadPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundLoginPacket ignored) {
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

    private void packetReceived (final ClientboundCustomPayloadPacket packet) {
        if (packet.getChannel().equals(SECRET_KEY)) {
            final byte[] bytes = packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final SecretPacket secretPacket = new SecretPacket().fromBytes(buf);
            this.initializationData = new InitializationData(secretPacket);

            this.socketAddress = new InetSocketAddress(
                    secretPacket.voiceHost.isBlank() ?
                            bot.options.host :
                            secretPacket.voiceHost,
                    initializationData.serverPort
            );

            this.socket = new ClientVoiceChatSocket();

            try {
                socket.open();
            } catch (final Exception e) {
                bot.logger.error(I18nUtilities.get("voicechat.failed_connecting"));
                bot.logger.error(e);
                return;
            }

            final Thread thread = new Thread(this, "Simple Voice Chat Thread");
            thread.start();
        } else if (packet.getChannel().equals(ADD_GROUP_KEY)) {
            final byte[] bytes = packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final ClientGroup group = ClientGroup.fromBytes(buf);

            groups.add(group);
        } else if (packet.getChannel().equals(REMOVE_GROUP_KEY)) {
            final byte[] bytes = packet.getData();
            final FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));

            final UUID id = buf.readUUID();

            groups.removeIf(group -> group.id().equals(id));
        }
    }

    @Override
    public void run () {
        sendToServer(new NetworkMessage(new AuthenticatePacket(initializationData.playerUUID, initializationData.secret)));

        while (running) {
            try {
                final NetworkMessage message = NetworkMessage.readPacket(socket.read(), initializationData);

                if (message == null) continue;

                if (message.packet instanceof final PingPacket pingPacket)
                    sendToServer(new NetworkMessage(pingPacket));
                else if (message.packet instanceof KeepAlivePacket)
                    sendToServer(new NetworkMessage(new KeepAlivePacket()));
                else if (message.packet instanceof AuthenticateAckPacket) {
                    sendToServer(new NetworkMessage(new ConnectionCheckPacket()));

                    bot.logger.log(
                            LogType.SIMPLE_VOICE_CHAT,
                            Component.translatable(
                                    I18nUtilities.get("voicechat.connected"),
                                    Component.text(socketAddress.toString())
                            )
                    );
                }
            } catch (final Exception e) {
                if (running) bot.logger.error(e);
                else break; // stop the thread
            }
        }
    }

    @SuppressWarnings("unused") // can be set through ServerEvalCommand
    public void joinGroup (final String group, final String password) {
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

    public void sendToServer (final NetworkMessage message) {
        try {
            socket.send(
                    message.writeClient(initializationData),
                    socketAddress
            );
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    private class VoiceChatSocketBase {
        private final byte[] BUFFER = new byte[4096];

        public RawUdpPacket read (final DatagramSocket socket) {
            if (socket.isClosed()) return null;
            try {
                final DatagramPacket packet = new DatagramPacket(BUFFER, BUFFER.length);

                socket.receive(packet);

                // Setting the timestamp after receiving the packet
                final long timestamp = System.currentTimeMillis();
                final byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());
                return new RawUdpPacket(data, packet.getSocketAddress(), timestamp);
            } catch (final Exception e) {
                if (!running) return null;

                bot.logger.error(e);
            }

            return null;
        }

    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        if (socket != null) socket.close();
        groups.clear();

        running = false;
    }

    private class ClientVoiceChatSocket extends VoiceChatSocketBase {
        private DatagramSocket socket;

        public void open () throws SocketException {
            this.socket = new DatagramSocket();
        }

        public RawUdpPacket read () {
            if (socket == null) {
                throw new IllegalStateException("Socket not opened yet");
            }
            return read(socket);
        }

        public void send (final byte[] data, final SocketAddress address) throws Exception {
            if (socket == null) {
                return; // Ignoring packet sending when socket isn't open yet
            }
            socket.send(new DatagramPacket(data, data.length, address));
        }

        public void close () {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        }
    }
}
