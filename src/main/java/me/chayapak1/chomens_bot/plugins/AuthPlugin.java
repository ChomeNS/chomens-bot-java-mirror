package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import javax.crypto.Cipher;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

public class AuthPlugin extends PlayersPlugin.Listener {
    private static final String ID = "chomens_bot_verify";

    private static PrivateKey PRIVATE_KEY;
    private static final Path PRIVATE_KEY_PATH = Path.of("private.key");
    private static final Path PUBLIC_KEY_PATH = Path.of("public.key");

    private static final String BEGIN_PRIVATE_KEY = "-----BEGIN CHOMENS BOT PRIVATE KEY-----";
    private static final String END_PRIVATE_KEY = "-----END CHOMENS BOT PRIVATE KEY-----";

    private static final String BEGIN_PUBLIC_KEY = "-----BEGIN CHOMENS BOT PUBLIC KEY-----";
    private static final String END_PUBLIC_KEY = "-----END CHOMENS BOT PUBLIC KEY-----";

    public static void init (Configuration config) {
        if (!config.ownerAuthentication.enabled) return;

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
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException | IllegalArgumentException e) {
            LoggerUtilities.error(e);
        }
    }

    private final Bot bot;

    public boolean isAuthenticating = false;
    public long startTime;

    public AuthPlugin (Bot bot) {
        this.bot = bot;

        if (!bot.config.ownerAuthentication.enabled) return;

        bot.executor.scheduleAtFixedRate(() -> {
            if (!isAuthenticating || !bot.config.ownerAuthentication.enabled) return;

            timeoutCheck();
            sendAuthRequestMessage();
        }, 500, 500, TimeUnit.MILLISECONDS);

        bot.addListener(new Bot.Listener() {
            @Override
            public void disconnected (DisconnectedEvent event) {
                AuthPlugin.this.disconnected();
            }
        });

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public boolean systemMessageReceived (Component component, String string, String ansi) {
                return AuthPlugin.this.systemMessageReceived(component);
            }
        });

        bot.players.addListener(this);
    }

    private String decrypt (byte[] data) throws Exception {
        final Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);

        final byte[] decryptedBytes = cipher.doFinal(data);

        return new String(decryptedBytes);
    }

    private void timeoutCheck () {
        if (System.currentTimeMillis() - startTime < bot.config.ownerAuthentication.timeout) return;

        final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

        if (target == null) return;

        bot.filterManager.add(target, "Authentication timed out");
    }

    private void sendAuthRequestMessage () {
        final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

        if (target == null) return;

        final Component component = Component
                .text(ID)
                .append(Component.text(UUIDUtilities.selector(bot.profile.getId())));

        bot.chat.tellraw(component, target.profile.getId());
    }

    @Override
    public void playerJoined (PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName) || !bot.options.useCore) return;

        startTime = System.currentTimeMillis();
        isAuthenticating = true;
    }

    private boolean systemMessageReceived (Component component) {
        if (!bot.config.ownerAuthentication.enabled) return true;

        if (!(component instanceof TextComponent textComponent)) return true;

        final String id = textComponent.content();

        if (!id.equals(ID)) return true;

        if (!isAuthenticating) return false;

        if (component.children().size() != 1) return true;

        if (!(component.children().getFirst() instanceof TextComponent dataComponent)) return true;

        final String data = dataComponent.content();

        try {
            final String decrypted = decrypt(Base64.getDecoder().decode(data));

            // what should i use here? should it be the ID?
            // or does it even matter?
            if (!decrypted.equals(ID)) return false;

            isAuthenticating = false;

            bot.logger.log(
                    LogType.AUTH,
                    Component
                            .text("Player has been verified")
                            .color(NamedTextColor.GREEN)
            );

            final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

            if (target == null) return false; // sad :(

            bot.chat.tellraw(
                    Component
                            .text("You have been verified")
                            .color(NamedTextColor.GREEN),
                    target.profile.getId()
            );
        } catch (Exception ignored) {}

        return false;
    }

    @Override
    public void playerLeft (PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName)) return;

        isAuthenticating = false;
    }

    private void disconnected () {
        isAuthenticating = false;
    }
}
