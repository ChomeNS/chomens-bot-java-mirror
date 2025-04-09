package me.chayapak1.chomens_bot.voiceChat;

import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.data.voiceChat.RawUdpPacket;
import me.chayapak1.chomens_bot.util.AESUtilities;
import me.chayapak1.chomens_bot.util.FriendlyByteBuf;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import me.chayapak1.chomens_bot.voiceChat.packets.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketAddress;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NetworkMessage {
    public static final byte MAGIC_BYTE = (byte) 0b11111111;

    public final long timestamp;
    public Packet<? extends Packet<?>> packet;
    public SocketAddress address;

    public NetworkMessage (final Packet<?> packet) {
        this(System.currentTimeMillis());
        this.packet = packet;
    }

    private NetworkMessage (final long timestamp) {
        this.timestamp = timestamp;
    }

    private static final Map<Byte, Class<? extends Packet<?>>> packetRegistry;

    static {
        packetRegistry = new HashMap<>();
        packetRegistry.put((byte) 0x1, MicPacket.class);
        //        packetRegistry.put((byte) 0x2, PlayerSoundPacket.class);
        //        packetRegistry.put((byte) 0x3, GroupSoundPacket.class);
        //        packetRegistry.put((byte) 0x4, LocationSoundPacket.class);
        packetRegistry.put((byte) 0x5, AuthenticatePacket.class);
        packetRegistry.put((byte) 0x6, AuthenticateAckPacket.class);
        packetRegistry.put((byte) 0x7, PingPacket.class);
        packetRegistry.put((byte) 0x8, KeepAlivePacket.class);
        packetRegistry.put((byte) 0x9, ConnectionCheckPacket.class);
        packetRegistry.put((byte) 0xA, ConnectionAckPacket.class);
    }

    public static NetworkMessage readPacket (final RawUdpPacket packet, final InitializationData initializationData) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        if (packet == null) return null;

        final byte[] data = packet.data();
        final FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

        if (b.readByte() != MAGIC_BYTE) return null;

        return readFromBytes(packet.socketAddress(), initializationData.secret, b.readByteArray(), System.currentTimeMillis());
    }

    private static NetworkMessage readFromBytes (final SocketAddress socketAddress, final UUID secret, final byte[] encryptedPayload, final long timestamp) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        final byte[] decrypt;
        try {
            decrypt = AESUtilities.decrypt(secret, encryptedPayload);
        } catch (final Exception e) {
            LoggerUtilities.error(e);
            return null;
        }

        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(decrypt));
        final byte packetType = buffer.readByte();
        final Class<? extends Packet<?>> packetClass = packetRegistry.get(packetType);
        if (packetClass == null) return null;
        final Packet<?> p = packetClass.getDeclaredConstructor().newInstance();

        final NetworkMessage message = new NetworkMessage(timestamp);
        message.address = socketAddress;
        message.packet = p.fromBytes(buffer);

        return message;
    }

    private static byte getPacketType (final Packet<? extends Packet<?>> packet) {
        for (final Map.Entry<Byte, Class<? extends Packet<?>>> entry : packetRegistry.entrySet()) {
            if (packet.getClass().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public byte[] writeClient (final InitializationData data) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final byte[] payload = write(data.secret);
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(1 + 16 + payload.length));
        buffer.writeByte(MAGIC_BYTE);
        buffer.writeUUID(data.playerUUID);
        buffer.writeByteArray(payload);

        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public byte[] write (final UUID secret) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        final byte type = getPacketType(packet);
        if (type < 0) {
            throw new IllegalArgumentException("Packet type not found");
        }

        buffer.writeByte(type);
        packet.toBytes(buffer);

        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return AESUtilities.encrypt(secret, bytes);
    }
}
