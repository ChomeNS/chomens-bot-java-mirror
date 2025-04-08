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
import me.chayapak1.chomens_bot.data.chomeNSMod.PayloadState;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

// This is inspired from the ChomeNS Bot Proxy which is in the JavaScript version of ChomeNS Bot.
public class ChomeNSModIntegrationPlugin implements ChatPlugin.Listener, PlayersPlugin.Listener, TickPlugin.Listener {
    private static final String ID = "chomens_mod";
    private static final int ENCODED_PAYLOAD_LENGTH = 31_000; // just 32767 trimmed "a bit"

    private static final Random RANDOM = new Random();

    public static final List<Class<? extends Packet>> SERVERBOUND_PACKETS = new ArrayList<>();

    static {
        SERVERBOUND_PACKETS.add(ServerboundSuccessfulHandshakePacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCoreCommandPacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCommandPacket.class);
    }

    private final Bot bot;

    private final PacketHandler handler;

    private final List<Listener> listeners = new ArrayList<>();

    public final List<PlayerEntry> connectedPlayers = new ArrayList<>();

    private final Map<PlayerEntry, Map<Integer, StringBuilder>> receivedParts = new HashMap<>();

    public ChomeNSModIntegrationPlugin (Bot bot) {
        this.bot = bot;
        this.handler = new PacketHandler(bot);

        bot.chat.addListener(this);
        bot.players.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onSecondTick () {
        tryHandshaking();
    }

    public void send (PlayerEntry target, Packet packet) {
        if (!connectedPlayers.contains(target) && !(packet instanceof ClientboundHandshakePacket))
            return; // LoL sus check

        final ByteBuf buf = Unpooled.buffer();

        buf.writeInt(packet.getId());
        packet.serialize(buf);

        final byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        try {
            final int messageId = RANDOM.nextInt();

            final String encrypted = Encryptor.encrypt(bytes, bot.config.chomeNSMod.password);

            final Iterable<String> split = Splitter.fixedLength(ENCODED_PAYLOAD_LENGTH).split(encrypted);

            int i = 1;

            for (String part : split) {
                final PayloadState state = i == Iterables.size(split)
                        ? PayloadState.DONE
                        : PayloadState.JOINING;

                final Component component = Component.translatable(
                        "",
                        Component.text(ID),
                        Component.text(messageId),
                        Component.text(state.ordinal()),
                        Component.text(part)
                );

                bot.chat.actionBar(component, target.profile.getId());

                i++;
            }
        } catch (Exception ignored) { }
    }

    private Packet deserialize (byte[] data) {
        final ByteBuf buf = Unpooled.wrappedBuffer(data);

        final int id = buf.readInt();

        final Class<? extends Packet> packetClass = SERVERBOUND_PACKETS.get(id);

        if (packetClass == null) return null;

        try {
            return packetClass.getDeclaredConstructor(ByteBuf.class).newInstance(buf);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        if (
                !(component instanceof TranslatableComponent translatableComponent) ||
                        !translatableComponent.key().isEmpty()
        ) return true;

        final List<TranslationArgument> arguments = translatableComponent.arguments();

        if (
                arguments.size() != 5 ||

                        !(arguments.get(0).asComponent() instanceof TextComponent idTextComponent) ||
                        !(arguments.get(1).asComponent() instanceof TextComponent uuidTextComponent) ||
                        !(arguments.get(2).asComponent() instanceof TextComponent messageIdTextComponent) ||
                        !(arguments.get(3).asComponent() instanceof TextComponent payloadStateTextComponent) ||
                        !(arguments.get(4).asComponent() instanceof TextComponent payloadTextComponent) ||

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

            if (!receivedParts.containsKey(player)) receivedParts.put(player, new HashMap<>());

            final Map<Integer, StringBuilder> playerReceivedParts = receivedParts.get(player);

            if (!playerReceivedParts.containsKey(messageId)) playerReceivedParts.put(messageId, new StringBuilder());

            final StringBuilder builder = playerReceivedParts.get(messageId);

            final String payload = payloadTextComponent.content();

            builder.append(payload);

            playerReceivedParts.put(messageId, builder);

            if (payloadState == PayloadState.DONE) {
                playerReceivedParts.remove(messageId);

                final byte[] decryptedFullPayload = Encryptor.decrypt(
                        builder.toString(),
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
        } catch (Exception ignored) { }

        return false;
    }

    private void tryHandshaking () {
        for (String username : bot.config.chomeNSMod.players) {
            final PlayerEntry target = bot.players.getEntry(username);

            if (target == null || connectedPlayers.contains(target)) continue;

            send(target, new ClientboundHandshakePacket());
        }
    }

    private void handlePacket (PlayerEntry player, Packet packet) {
        if (packet instanceof ServerboundSuccessfulHandshakePacket) {
            connectedPlayers.removeIf(eachPlayer -> eachPlayer.equals(player));
            connectedPlayers.add(player);
        }

        handler.handlePacket(player, packet);

        for (Listener listener : listeners) listener.packetReceived(player, packet);
    }

    @Override
    public void playerLeft (PlayerEntry target) {
        connectedPlayers.removeIf(player -> player.equals(target));
        receivedParts.remove(target);
    }

    @SuppressWarnings("unused")
    public interface Listener {
        default void packetReceived (PlayerEntry player, Packet packet) { }
    }
}
