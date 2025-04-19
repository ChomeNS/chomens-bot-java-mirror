package me.chayapak1.chomens_bot.plugins;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.Encryptor;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.PacketHandler;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundHandshakePacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCoreCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundSuccessfulHandshakePacket;
import me.chayapak1.chomens_bot.data.chomeNSMod.PayloadMetadata;
import me.chayapak1.chomens_bot.data.chomeNSMod.PayloadState;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.Ascii85;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// This is inspired from the ChomeNS Bot Proxy which is in the JavaScript version of ChomeNS Bot.
public class ChomeNSModIntegrationPlugin implements Listener {
    private static final String ID = "chomens_mod";

    private static final int ASCII85_CHUNK_SIZE = 31_000; // just 32767 trimmed "a bit"
    private static final int EXTRAS_CHUNK_SIZE = 32_500; // just 32767 trimmed a bit

    private static final long NONCE_EXPIRATION_MS = 30 * 1000; // 30 seconds

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final List<Class<? extends Packet>> SERVERBOUND_PACKETS = new ArrayList<>();

    static {
        SERVERBOUND_PACKETS.add(ServerboundSuccessfulHandshakePacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCoreCommandPacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCommandPacket.class);
    }

    private final Bot bot;

    private final PacketHandler handler;

    public final List<PlayerEntry> connectedPlayers = Collections.synchronizedList(new ArrayList<>());

    private final Map<PlayerEntry, Map<Integer, StringBuilder>> receivedParts = new ConcurrentHashMap<>();

    private final List<PayloadMetadata> seenMetadata = Collections.synchronizedList(new ArrayList<>());

    public ChomeNSModIntegrationPlugin (final Bot bot) {
        this.bot = bot;
        this.handler = new PacketHandler(bot);

        bot.extrasMessenger.registerChannel(ID);

        bot.listener.addListener(this);
    }

    @Override
    public void onSecondTick () {
        tryHandshaking();

        seenMetadata.removeIf(
                metadata -> System.currentTimeMillis() - metadata.timestamp() > NONCE_EXPIRATION_MS
        );
    }

    public void send (final PlayerEntry target, final Packet packet) {
        if (!connectedPlayers.contains(target) && !(packet instanceof ClientboundHandshakePacket)) return;

        final ByteBuf buf = Unpooled.buffer();

        final PayloadMetadata metadata = generateMetadata();
        metadata.serialize(buf);

        buf.writeInt(packet.getId());
        packet.serialize(buf);

        final byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        try {
            final int messageId = RANDOM.nextInt();

            final byte[] encrypted = Encryptor.encrypt(bytes, bot.config.chomeNSMod.password);

            // FIXME: not sure how to implement on the mod side yet
            final boolean supportsExtrasMessaging = false; // bot.extrasMessenger.isSupported;

            if (supportsExtrasMessaging) {
                // TODO: test this
                // TODO: implement receiver
                final List<byte[]> chunks = new ArrayList<>();

                for (int i = 0; i < encrypted.length; i += EXTRAS_CHUNK_SIZE) {
                    final int end = Math.min(encrypted.length, i + EXTRAS_CHUNK_SIZE);
                    final byte[] chunk = Arrays.copyOfRange(encrypted, i, end);
                    chunks.add(chunk);
                }

                int i = 1;

                for (final byte[] chunk : chunks) {
                    final PayloadState state = i == chunks.size()
                            ? PayloadState.DONE
                            : PayloadState.JOINING;

                    final ByteBuf extrasBuf = Unpooled.buffer();

                    extrasBuf.writeInt(messageId);
                    extrasBuf.writeShort(state.ordinal()); // short or byte?
                    extrasBuf.writeBytes(chunk);

                    final byte[] extrasBytes = new byte[buf.readableBytes()];
                    extrasBuf.readBytes(extrasBytes);

                    bot.extrasMessenger.sendPayload(ID, extrasBytes);

                    i++;
                }
            } else {
                final String encryptedAscii85 = Ascii85.encode(encrypted);

                final Iterable<String> chunks = Splitter.fixedLength(ASCII85_CHUNK_SIZE).split(encryptedAscii85);

                int i = 1;

                for (final String chunk : chunks) {
                    final PayloadState state = i == Iterables.size(chunks)
                            ? PayloadState.DONE
                            : PayloadState.JOINING;

                    final Component component = Component.translatable(
                            "",
                            Component.text(ID),
                            Component.text(messageId),
                            Component.text(state.ordinal()),
                            Component.text(chunk)
                    );

                    bot.chat.actionBar(component, target.profile.getId());

                    i++;
                }
            }
        } catch (final Exception ignored) { }
    }

    private PayloadMetadata generateMetadata () {
        final byte[] nonce = new byte[8];
        RANDOM.nextBytes(nonce);

        final long timestamp = System.currentTimeMillis();

        return new PayloadMetadata(nonce, timestamp);
    }

    private boolean isValidPayload (final PayloadMetadata metadata) {
        // check if the timestamp is less than the expiration time
        if (System.currentTimeMillis() - metadata.timestamp() > NONCE_EXPIRATION_MS) return false;

        // check if nonce is replayed in case the server owner
        // is not being very nice and decided to pull out
        // a replay attack
        final boolean valid = !seenMetadata.contains(metadata);

        if (valid) seenMetadata.add(metadata);

        return valid;
    }

    private Packet deserialize (final byte[] data) {
        final ByteBuf buf = Unpooled.wrappedBuffer(data);

        final PayloadMetadata metadata = PayloadMetadata.deserialize(buf);

        if (!isValidPayload(metadata)) {
            bot.logger.log(
                    LogType.INFO,
                    Component.translatable(
                            "Ignoring suspected replay attack payload with metadata: %s",
                            Component.text(metadata.toString()) // PayloadMetadata has toString()
                    )
            );
            return null;
        }

        final int id = buf.readInt();

        final Class<? extends Packet> packetClass = SERVERBOUND_PACKETS.get(id);

        if (packetClass == null) return null;

        try {
            return packetClass.getDeclaredConstructor(ByteBuf.class).newInstance(buf);
        } catch (final NoSuchMethodException | InvocationTargetException | InstantiationException |
                       IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public boolean onSystemMessageReceived (final Component component, final String string, final String ansi) {
        if (
                !(component instanceof final TranslatableComponent translatableComponent) ||
                        !translatableComponent.key().isEmpty()
        ) return true;

        final List<TranslationArgument> arguments = translatableComponent.arguments();

        if (
                arguments.size() != 5 ||

                        !(arguments.get(0).asComponent() instanceof final TextComponent idTextComponent) ||
                        !(arguments.get(1).asComponent() instanceof final TextComponent uuidTextComponent) ||
                        !(arguments.get(2).asComponent() instanceof final TextComponent messageIdTextComponent) ||
                        !(arguments.get(3).asComponent() instanceof final TextComponent payloadStateTextComponent) ||
                        !(arguments.get(4).asComponent() instanceof final TextComponent payloadTextComponent) ||

                        !idTextComponent.content().equals(ID)
        ) return true;

        try {
            final UUID uuid = UUIDUtilities.tryParse(uuidTextComponent.content());

            if (uuid == null) return true;

            final PlayerEntry player = bot.players.getEntry(uuid);

            if (player == null) return false;

            final int messageId = Integer.parseInt(messageIdTextComponent.content());
            final int payloadStateIndex = Integer.parseInt(payloadStateTextComponent.content());

            final PayloadState payloadState = PayloadState.values()[payloadStateIndex];

            receivedParts.putIfAbsent(player, new ConcurrentHashMap<>());

            final Map<Integer, StringBuilder> playerReceivedParts = receivedParts.get(player);

            if (!playerReceivedParts.containsKey(messageId)) playerReceivedParts.put(messageId, new StringBuilder());

            final StringBuilder builder = playerReceivedParts.get(messageId);

            final String payload = payloadTextComponent.content();

            builder.append(payload);

            playerReceivedParts.put(messageId, builder);

            if (payloadState == PayloadState.DONE) {
                playerReceivedParts.remove(messageId);

                final byte[] decryptedFullPayload = Encryptor.decrypt(
                        Ascii85.decode(builder.toString()),
                        bot.config.chomeNSMod.password
                );

                final Packet packet = deserialize(decryptedFullPayload);

                if (
                        packet == null ||
                                (
                                        !(packet instanceof ServerboundSuccessfulHandshakePacket) &&
                                                !connectedPlayers.contains(player)
                                )
                ) return false;

                handlePacket(player, packet);
            }
        } catch (final Exception ignored) { }

        return false;
    }

    private void tryHandshaking () {
        for (final String username : bot.config.chomeNSMod.players) {
            final PlayerEntry target = bot.players.getEntry(username);

            if (target == null || connectedPlayers.contains(target)) continue;

            send(target, new ClientboundHandshakePacket());
        }
    }

    private void handlePacket (final PlayerEntry player, final Packet packet) {
        if (packet instanceof ServerboundSuccessfulHandshakePacket) {
            connectedPlayers.removeIf(eachPlayer -> eachPlayer.equals(player));
            connectedPlayers.add(player);
        }

        handler.handlePacket(player, packet);

        bot.listener.dispatch(listener -> listener.onChomeNSModPacketReceived(player, packet));
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        connectedPlayers.removeIf(player -> player.equals(target));
        receivedParts.remove(target);
    }
}
