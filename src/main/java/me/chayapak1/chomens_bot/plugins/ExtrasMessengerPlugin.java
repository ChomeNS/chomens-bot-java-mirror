package me.chayapak1.chomens_bot.plugins;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExtrasMessengerPlugin extends Bot.Listener {
    private static final Key EXTRAS_REGISTER_KEY = Key.key("extras", "register");
    private static final Key EXTRAS_UNREGISTER_KEY = Key.key("extras", "unregister");
    private static final Key EXTRAS_MESSAGE_KEY = Key.key("extras", "message");

    private final List<Listener> listeners = new ArrayList<>();

    private final Bot bot;

    private final String chomens_namespace;

    public ExtrasMessengerPlugin (Bot bot) {
        this.bot = bot;
        this.chomens_namespace = bot.config.namespace + ":"; // Ex. chomens_bot: (then it will be appended by channel)

        bot.addListener(this);
    }

    @Override
    public void connected (ConnectedEvent event) {
        // TODO: don't register these things on connected but on the
        //       custom payload packet instead so it only registers
        //       for servers that support this
        final List<String> channels = new ArrayList<>();

        channels.add(EXTRAS_REGISTER_KEY.asString());
        channels.add(EXTRAS_UNREGISTER_KEY.asString());
        channels.add(EXTRAS_MESSAGE_KEY.asString());

        bot.session.send(
                new ServerboundCustomPayloadPacket(
                        Key.key("minecraft", "register"),
                        String.join("\u0000", channels).getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundCustomPayloadPacket t_packet) packetReceived(t_packet);
    }

    public void packetReceived (ClientboundCustomPayloadPacket packet) {
        final Key packetChannel = packet.getChannel();

        if (!packetChannel.equals(EXTRAS_MESSAGE_KEY)) return;

        final ByteBuf buf = Unpooled.wrappedBuffer(packet.getData());

        final String channelName = readString(buf);

        if (!channelName.startsWith(chomens_namespace)) return;

        final UUID uuid = readUUID(buf);

        final byte[] data = readByteArrayToEnd(buf);

        for (Listener listener : listeners) listener.onMessage(uuid, data);
    }

    public void sendPayload (String name, byte[] data) {
        final ByteBuf buf = Unpooled.buffer();

        writeString(buf, chomens_namespace + name);
        buf.writeBytes(data);

        final byte[] byteArray = readByteArrayToEnd(buf);

        bot.session.send(
                new ServerboundCustomPayloadPacket(
                        EXTRAS_MESSAGE_KEY,
                        byteArray
                )
        );
    }

    // TODO: automatically register the already registered channels on reconnect
    public void registerChannel (String channel) {
        final ByteBuf buf = Unpooled.buffer();

        writeString(buf, chomens_namespace + channel);

        bot.session.send(
                new ServerboundCustomPayloadPacket(
                        EXTRAS_REGISTER_KEY,
                        readByteArrayToEnd(buf)
                )
        );
    }

    private void writeString (ByteBuf input, String string) {
        final byte[] bytesString = string.getBytes(StandardCharsets.US_ASCII);

        input.writeByte(bytesString.length);
        input.writeBytes(bytesString);
    }

    private String readString (ByteBuf input) {
        final int length = input.readUnsignedByte();

        final byte[] buf = new byte[length];

        input.readBytes(buf);

        return new String(buf, StandardCharsets.US_ASCII);
    }

    private UUID readUUID (ByteBuf input) {
        final long mostSignificant = input.readLong();
        final long leastSignificant = input.readLong();

        return new UUID(mostSignificant, leastSignificant);
    }

    private byte[] readByteArrayToEnd (ByteBuf input) {
        final byte[] bytes = new byte[input.readableBytes()];

        input.readBytes(bytes);

        return bytes;
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void onMessage (UUID sender, byte[] message) {}
    }
}
