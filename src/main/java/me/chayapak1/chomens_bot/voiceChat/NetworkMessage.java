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

    public NetworkMessage(Packet<?> packet) {
        this(System.currentTimeMillis());
        this.packet = packet;
    }

    private NetworkMessage(long timestamp) {
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

    public static NetworkMessage readPacket (RawUdpPacket packet, InitializationData initializationData) throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        if (packet == null) return null;

        final byte[] data = packet.data();
        final FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));

        if (b.readByte() != MAGIC_BYTE) return null;

        return readFromBytes(packet.socketAddress(), initializationData.secret, b.readByteArray(), System.currentTimeMillis());
    }

    private static NetworkMessage readFromBytes(SocketAddress socketAddress, UUID secret, byte[] encryptedPayload, long timestamp) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        byte[] decrypt;
        try {
            decrypt = AESUtilities.decrypt(secret, encryptedPayload);
        } catch (Exception e) {
            LoggerUtilities.error(e);
            return null;
        }

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(decrypt));
        byte packetType = buffer.readByte();
        final Class<? extends Packet<?>> packetClass = packetRegistry.get(packetType);
        if (packetClass == null) return null;
        Packet<?> p = packetClass.getDeclaredConstructor().newInstance();

        NetworkMessage message = new NetworkMessage(timestamp);
        message.address = socketAddress;
        message.packet = p.fromBytes(buffer);

        return message;
    }

    private static byte getPacketType(Packet<? extends Packet<?>> packet) {
        for (Map.Entry<Byte, Class<? extends Packet<?>>> entry : packetRegistry.entrySet()) {
            if (packet.getClass().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return -1;
    }

    public byte[] writeClient(InitializationData data) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        byte[] payload = write(data.secret);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer(1 + 16 + payload.length));
        buffer.writeByte(MAGIC_BYTE);
        buffer.writeUUID(data.playerUUID);
        buffer.writeByteArray(payload);

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return bytes;
    }

    public byte[] write(UUID secret) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());

        byte type = getPacketType(packet);
        if (type < 0) {
            throw new IllegalArgumentException("Packet type not found");
        }

        buffer.writeByte(type);
        packet.toBytes(buffer);

        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        return AESUtilities.encrypt(secret, bytes);
    }
}
