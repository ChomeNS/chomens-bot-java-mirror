package me.chayapak1.chomens_bot.plugins;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chomeNSMod.Packet;
import me.chayapak1.chomens_bot.chomeNSMod.PacketHandler;
import me.chayapak1.chomens_bot.chomeNSMod.Types;
import me.chayapak1.chomens_bot.chomeNSMod.clientboundPackets.ClientboundHandshakePacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundRunCoreCommandPacket;
import me.chayapak1.chomens_bot.chomeNSMod.serverboundPackets.ServerboundSuccessfulHandshakePacket;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.Ascii85;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.tuple.Pair;

import javax.crypto.Cipher;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.stream.Stream;

// This is inspired from the ChomeNS Bot Proxy which is in the JavaScript version of ChomeNS Bot.
public class ChomeNSModIntegrationPlugin implements ChatPlugin.Listener, PlayersPlugin.Listener, TickPlugin.Listener {
    private static final String ID = "chomens_mod";

    public static final List<Class<? extends Packet>> SERVERBOUND_PACKETS = new ArrayList<>();

    static {
        SERVERBOUND_PACKETS.add(ServerboundSuccessfulHandshakePacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCoreCommandPacket.class);
        SERVERBOUND_PACKETS.add(ServerboundRunCommandPacket.class);
    }

    private static PrivateKey PRIVATE_KEY;

    private static final Map<String, PublicKey> CLIENT_PUBLIC_KEYS = new HashMap<>();
    private static final Path CLIENT_PUBLIC_KEYS_PATH = Path.of("client_public_keys");

    private static final Path PRIVATE_KEY_PATH = Path.of("private.key");
    private static final Path PUBLIC_KEY_PATH = Path.of("public.key");

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN CHOMENS BOT PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END CHOMENS BOT PRIVATE KEY-----";

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN CHOMENS BOT PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END CHOMENS BOT PUBLIC KEY-----";

    public static void init () {
        try {
            // let's only check for the private key here
            if (!Files.exists(PRIVATE_KEY_PATH)) {
                final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);

                final KeyPair pair = keyGen.generateKeyPair();

                // write the keys
                // (note: no newline split is intentional)
                final String encodedPrivateKey =
                        BEGIN_PRIVATE_KEY + "\n" +
                                Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()) +
                                "\n" + END_PRIVATE_KEY;
                final String encodedPublicKey =
                        BEGIN_PUBLIC_KEY + "\n" +
                                Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()) +
                                "\n" + END_PUBLIC_KEY;

                final BufferedWriter privateKeyWriter = Files.newBufferedWriter(PRIVATE_KEY_PATH);
                privateKeyWriter.write(encodedPrivateKey);
                privateKeyWriter.close();

                final BufferedWriter publicKeyWriter = Files.newBufferedWriter(PUBLIC_KEY_PATH);
                publicKeyWriter.write(encodedPublicKey);
                publicKeyWriter.close();
            }

            // is this a good way to remove the things?
            final String privateKeyString = new String(Files.readAllBytes(PRIVATE_KEY_PATH))
                    .replace(BEGIN_PRIVATE_KEY + "\n", "")
                    .replace("\n" + END_PRIVATE_KEY, "")
                    .replace("\n", "")
                    .trim();

            final byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyString);
            final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PRIVATE_KEY = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));

            // lol this is so messy
            if (Files.isDirectory(CLIENT_PUBLIC_KEYS_PATH)) {
                try (final Stream<Path> files = Files.list(CLIENT_PUBLIC_KEYS_PATH)) {
                    for (Path path : files.toList()) {
                        try {
                            final String publicKeyString = new String(Files.readAllBytes(path))
                                    .replace(BEGIN_PUBLIC_KEY + "\n", "")
                                    .replace("\n" + END_PUBLIC_KEY, "")
                                    .replace("\n", "")
                                    .trim();

                            final byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyString);
                            final KeyFactory clientKeyFactory = KeyFactory.getInstance("RSA");

                            String username = path.getFileName().toString();

                            if (username.contains(".")) username = username.substring(0, username.lastIndexOf("."));

                            CLIENT_PUBLIC_KEYS.put(
                                    username,
                                    clientKeyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes))
                            );
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            LoggerUtilities.error(e);
        }
    }

    private final Bot bot;

    private final PacketHandler handler;

    private final List<Listener> listeners = new ArrayList<>();

    public final List<PlayerEntry> connectedPlayers = new ArrayList<>();

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

    public byte[] decrypt (byte[] data) throws Exception {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);

        return cipher.doFinal(data);
    }

    public String encrypt (String player, byte[] data) throws Exception {
        final PublicKey publicKey = CLIENT_PUBLIC_KEYS.get(player);

        if (publicKey == null) return null;

        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        final byte[] encryptedBytes = cipher.doFinal(data);

        return Ascii85.encode(encryptedBytes);
    }

    public void send (PlayerEntry target, Packet packet) {
        if (!connectedPlayers.contains(target) && !(packet instanceof ClientboundHandshakePacket)) return; // LoL sus check

        final ByteBuf buf = Unpooled.buffer();

        buf.writeInt(packet.getId());
        packet.serialize(buf);

        final byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        try {
            final String encrypted = encrypt(target.profile.getName(), bytes);

            final Component component = Component.translatable(
                    "",
                    Component.text(ID),
                    Component.text(encrypted)
            );

            bot.chat.actionBar(component, target.profile.getId());
        } catch (Exception ignored) {}
    }

    private Pair<PlayerEntry, Packet> deserialize (byte[] data) {
        final ByteBuf buf = Unpooled.wrappedBuffer(data);

        final UUID uuid = Types.readUUID(buf);

        final PlayerEntry player = bot.players.getEntry(uuid);

        if (player == null) return null;

        final int id = buf.readInt();

        final Class<? extends Packet> packetClass = SERVERBOUND_PACKETS.get(id);

        if (packetClass == null) return null;

        try {
            return Pair.of(player, packetClass.getDeclaredConstructor(ByteBuf.class).newInstance(buf));
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            return null;
        }
    }

    @Override
    public boolean systemMessageReceived (Component component, String string, String ansi) {
        if (!(component instanceof TextComponent textComponent)) return true;

        final String id = textComponent.content();

        if (
                !id.equals(ID) ||
                        component.children().size() != 1 ||
                        !(component.children().getFirst() instanceof TextComponent dataComponent)
        ) return true;

        final String data = dataComponent.content();

        try {
            final byte[] decrypted = decrypt(Ascii85.decode(data));

            final Pair<PlayerEntry, Packet> deserialized = deserialize(decrypted);

            if (deserialized == null) return false;

            final PlayerEntry player = deserialized.getKey();
            final Packet packet = deserialized.getValue();

            handlePacket(player, packet);
        } catch (Exception ignored) {}

        return false;
    }

    private void tryHandshaking () {
        // is looping through the usernames from the client public keys list a good idea?
        for (String username : CLIENT_PUBLIC_KEYS.keySet()) {
            final PlayerEntry target = bot.players.getEntry(username);

            if (target == null || connectedPlayers.contains(target)) continue;

            send(target, new ClientboundHandshakePacket());
        }
    }

    private void handlePacket (PlayerEntry player, Packet packet) {
        if (packet instanceof ServerboundSuccessfulHandshakePacket) {
            connectedPlayers.remove(player);
            connectedPlayers.add(player);
        }

        handler.handlePacket(player, packet);

        for (Listener listener : listeners) listener.packetReceived(player, packet);
    }

    @Override
    public void playerLeft (PlayerEntry target) {
        if (!connectedPlayers.contains(target)) return;

        connectedPlayers.remove(target);
    }

    public interface Listener {
        default void packetReceived (PlayerEntry player, Packet packet) {}
    }
}
