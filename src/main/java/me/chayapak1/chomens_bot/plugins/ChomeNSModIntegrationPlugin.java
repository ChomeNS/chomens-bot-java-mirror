package me.chayapak1.chomens_bot.plugins;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.Encryptor;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.PacketHandler;
import me.chayapak1.chomens_bot.chomeNSMod.Types;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundHandshakePacket;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundMessagePacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCoreCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundSuccessfulHandshakePacket;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chomeNSMod.PayloadMetadata;
import me.chayapak1.chomens_bot.data.chomeNSMod.PayloadState;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.Ascii85;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.lang.reflect.InvocationTargetException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// This is inspired by the ChomeNS Bot Proxy, which is in the JavaScript version of ChomeNS Bot.
public class ChomeNSModIntegrationPlugin implements Listener {
    private static final String ID = "chomens_mod";

    private static final int EXTRAS_MESSAGING_CHUNK_SIZE = 32_600;
    private static final int ACTION_BAR_CHUNK_SIZE = 31_000 / 2; // some Magical Number

    private static final long NONCE_EXPIRATION_MS = 30 * 1000; // 30 seconds

    private static final SecureRandom RANDOM = new SecureRandom();

    public static final List<Class<? extends Packet>> SERVERBOUND_PACKETS = ObjectList.of(
            ServerboundSuccessfulHandshakePacket.class,
            ServerboundRunCoreCommandPacket.class,
            ServerboundRunCommandPacket.class
    );

    private final Bot bot;

    private final PacketHandler handler;

    public final List<PlayerEntry> connectedPlayers = Collections.synchronizedList(new ObjectArrayList<>());

    private final Map<PlayerEntry, Map<Integer, ByteBuf>> receivedParts = new ConcurrentHashMap<>();

    private final List<PayloadMetadata> seenMetadata = Collections.synchronizedList(new ObjectArrayList<>());

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

        final byte[] rawBytes = new byte[buf.readableBytes()];
        buf.readBytes(rawBytes);

        final boolean shouldUseExtrasMessenger = bot.extrasMessenger.isSupported;

        try {
            final int messageId = RANDOM.nextInt();

            final List<byte[]> chunks = new ArrayList<>();

            final int chunkSizeToUse = shouldUseExtrasMessenger ? EXTRAS_MESSAGING_CHUNK_SIZE : ACTION_BAR_CHUNK_SIZE;

            for (int i = 0; i < rawBytes.length; i += chunkSizeToUse) {
                final int end = Math.min(rawBytes.length, i + chunkSizeToUse);
                final byte[] chunk = Arrays.copyOfRange(rawBytes, i, end);
                chunks.add(chunk);
            }

            if (chunks.size() > 512) {
                bot.logger.error(
                        Component.translatable(
                                "Chunk is too large (%s) while trying to send packet %s to %s!",
                                Component.text(chunks.size()),
                                Component.text(packet.toString()),
                                Component.text(target.profile.getIdAsString())
                        )
                );
                return;
            }

            int i = 1;

            for (final byte[] chunk : chunks) {
                final PayloadState state = i == chunks.size()
                        ? PayloadState.DONE
                        : PayloadState.JOINING;

                final ByteBuf toSendBuf = Unpooled.buffer();

                toSendBuf.writeInt(messageId);
                toSendBuf.writeShort(state.ordinal()); // short or byte?
                toSendBuf.writeBytes(chunk);

                final byte[] toSendBytes = new byte[toSendBuf.readableBytes()];
                toSendBuf.readBytes(toSendBytes);

                final byte[] encrypted = Encryptor.encrypt(toSendBytes, bot.config.chomeNSMod.password);

                if (shouldUseExtrasMessenger) {
                    bot.extrasMessenger.sendPayload(ID, encrypted);
                } else {
                    final String ascii85EncryptedPayload = Ascii85.encode(encrypted);

                    final Component component = Component.translatable(
                            "",
                            Component.text(ID),
                            Component.text(ascii85EncryptedPayload)
                    );

                    bot.chat.actionBar(component, target.profile.getId());
                }

                i++;
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
                            I18nUtilities.get("chomens_mod.replay_attack"),
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
    public boolean onSystemMessageReceived (
            final Component component,
            final ChatPacketType packetType,
            final String string,
            final String ansi
    ) {
        if (
                packetType != ChatPacketType.SYSTEM
                        || !(component instanceof final TranslatableComponent translatableComponent)
                        || !translatableComponent.key().isEmpty()
        ) return true;

        final List<TranslationArgument> arguments = translatableComponent.arguments();

        if (
                arguments.size() != 2 ||

                        !(arguments.get(0).asComponent() instanceof final TextComponent idTextComponent) ||
                        !(arguments.get(1).asComponent() instanceof final TextComponent payloadTextComponent) ||

                        !idTextComponent.content().equals(ID)
        ) return true;

        try {
            final byte[] decrypted = Encryptor.decrypt(
                    Ascii85.decode(payloadTextComponent.content()),
                    bot.config.chomeNSMod.password
            );

            handleData(decrypted, false, null);
        } catch (final Exception ignored) { }

        return false;
    }

    @Override
    public void onExtrasMessageReceived (final UUID sender, final byte[] message) {
        try {
            final byte[] decrypted = Encryptor.decrypt(
                    message,
                    bot.config.chomeNSMod.password
            );

            handleData(decrypted, true, sender);
        } catch (final Exception ignored) { }
    }

    private void handleData (final byte[] data, final boolean isFromExtrasMessenger, final UUID extrasSenderUUID) {
        final ByteBuf chunkBuf = Unpooled.wrappedBuffer(data);

        final UUID uuid = isFromExtrasMessenger
                ? extrasSenderUUID
                : Types.readUUID(chunkBuf);

        final PlayerEntry player = bot.players.getEntry(uuid);
        if (player == null) return;

        final int messageId = chunkBuf.readInt();
        final short payloadStateIndex = chunkBuf.readShort();

        final PayloadState payloadState = PayloadState.values()[payloadStateIndex];

        receivedParts.putIfAbsent(player, new ConcurrentHashMap<>());

        final Map<Integer, ByteBuf> playerReceivedParts = receivedParts.get(player);

        if (!playerReceivedParts.containsKey(messageId)) playerReceivedParts.put(messageId, Unpooled.buffer());

        final ByteBuf buf = playerReceivedParts.get(messageId);

        buf.writeBytes(chunkBuf); // the remaining is the chunk since we read all of them

        playerReceivedParts.put(messageId, buf);

        if (payloadState == PayloadState.DONE) {
            playerReceivedParts.remove(messageId);

            final byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            final Packet packet = deserialize(bytes);

            if (
                    packet == null ||
                            (!(packet instanceof ServerboundSuccessfulHandshakePacket) &&
                                    !connectedPlayers.contains(player))
            ) return;

            handlePacket(player, packet);
        }
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

    public void sendMessage (final PlayerEntry target, final Component message) {
        send(target, new ClientboundMessagePacket(message));
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        connectedPlayers.removeIf(player -> player.equals(target));
        receivedParts.remove(target);
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        connectedPlayers.clear();
        receivedParts.clear();
        seenMetadata.clear();
    }
}
