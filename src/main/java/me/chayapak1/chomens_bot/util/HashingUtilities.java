package me.chayapak1.chomens_bot.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.keys.Key;
import me.chayapak1.chomens_bot.data.keys.KeysData;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HashingUtilities {
    public static final KeyManager KEY_MANAGER = new KeyManager();
    public static final Map<Long, Pair<TrustLevel, String>> discordHashes = new ConcurrentHashMap<>();

    public static void init () {
    }

    public static String getHash (final String key, final String prefix, final PlayerEntry sender) {
        final long time = System.currentTimeMillis() / 5_000;

        final String hashInput = sender.profile.getIdAsString() + prefix + time + key;

        return Hashing.sha256()
                .hashString(hashInput, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 16);
    }

    private static String getFixedHashInput (final String input) {
        String sanitizedInput = input;

        // removes reset section sign
        if (input.length() == (16 * 2) + 2 && input.endsWith("§r"))
            sanitizedInput = input.substring(0, input.length() - 2);

        // removes all the section signs
        sanitizedInput = sanitizedInput.replace("§", "");

        return sanitizedInput;
    }

    public static TrustLevel getPlayerHashTrustLevel (
            final String input,
            final String prefix,
            final PlayerEntry sender
    ) {
        final String fixedInput = getFixedHashInput(input);

        final List<KeysData> keys = KEY_MANAGER.keys;

        synchronized (keys) {
            for (final KeysData keysData : keys) {
                for (final Key keyObject : keysData.keys()) {
                    final String hashed = getHash(keyObject.key(), prefix, sender);
                    if (fixedInput.equals(hashed)) return keyObject.trustLevel();
                }
            }
        }

        // not TrustLevel.PUBLIC !
        return null;
    }

    public static boolean isCorrectDiscordHash (final String input) {
        final String fixedInput = getFixedHashInput(input);

        for (final Pair<TrustLevel, String> pair : discordHashes.values()) {
            if (pair.getValue().equals(fixedInput)) return true;
        }

        return false;
    }

    public static TrustLevel getDiscordHashTrustLevel (final String input) {
        for (final Map.Entry<Long, Pair<TrustLevel, String>> entry : new ArrayList<>(discordHashes.entrySet())) {
            final Pair<TrustLevel, String> pair = entry.getValue();

            if (!pair.getRight().equals(input)) continue;

            discordHashes.remove(entry.getKey());

            return pair.getLeft();
        }

        return TrustLevel.PUBLIC;
    }

    public static TrustLevel getTrustLevel (final String input, final String prefix, final PlayerEntry sender) {
        final TrustLevel playerHashTrustLevel = getPlayerHashTrustLevel(input, prefix, sender);

        if (playerHashTrustLevel != null) return playerHashTrustLevel;
        else if (isCorrectDiscordHash(input)) return getDiscordHashTrustLevel(input);
        else return TrustLevel.PUBLIC;
    }

    public static String generateDiscordHash (final long userId, final TrustLevel trustLevel) {
        // i wouldn't say it's a hash, it's just a random string
        final String string = RandomStringUtilities.generate(16);

        discordHashes.putIfAbsent(userId, Pair.of(trustLevel, string));

        return discordHashes.get(userId).getRight();
    }

    public static class KeyManager {
        private static final Path KEY_PATH = Path.of("keys.json");
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        public List<KeysData> keys = null;

        public KeyManager () {
            try {
                initialLoad();
            } catch (final IOException e) {
                LoggerUtilities.error("Failed to load the keys!");
                LoggerUtilities.error(e);

                return;
            }

            Main.EXECUTOR.scheduleAtFixedRate(this::write, 1, 1, TimeUnit.MINUTES);
            Runtime.getRuntime().addShutdownHook(new Thread(this::write));
        }

        public @Nullable String generate (
                final TrustLevel level,
                final String userId,
                final boolean force,
                final String alreadyExistsMessage // so it's flexible
        ) throws IllegalStateException {
            if (keys == null) return null;

            // is this useless? although it prevents the synchronization on non-final field warning by IDEA
            final List<KeysData> keys = this.keys;

            KeysData data = null;

            synchronized (keys) {
                for (final KeysData keysData : keys) {
                    if (keysData.userId().equals(userId)) {
                        data = keysData;
                        break;
                    }
                }
            }

            final String generatedKey = RandomStringUtilities.generate(48);

            if (data == null) {
                data = new KeysData(new ArrayList<>(), userId);

                data.keys().add(
                        new Key(
                                level,
                                generatedKey,
                                System.currentTimeMillis()
                        )
                );

                keys.add(data);
            } else {
                for (final Key key : new ArrayList<>(data.keys())) {
                    if (!key.trustLevel().equals(level)) continue;

                    if (!force) throw new IllegalStateException(alreadyExistsMessage);

                    data.keys().remove(key);
                }

                data.keys().add(
                        new Key(
                                level,
                                generatedKey,
                                System.currentTimeMillis()
                        )
                );
            }

            write();

            return generatedKey;
        }

        private void initialLoad () throws IOException {
            if (Files.exists(KEY_PATH)) {
                try (final BufferedReader reader = Files.newBufferedReader(KEY_PATH)) {
                    keys = Collections.synchronizedList(
                            OBJECT_MAPPER.readValue(
                                    reader,
                                    OBJECT_MAPPER
                                            .getTypeFactory()
                                            .constructCollectionType(ObjectArrayList.class, KeysData.class)
                            )
                    );
                }
            } else {
                Files.createFile(KEY_PATH);

                keys = Collections.synchronizedList(new ObjectArrayList<>());
            }
        }

        private void write () {
            try (
                    final BufferedWriter writer = Files.newBufferedWriter(
                            KEY_PATH,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                    )
            ) {
                writer.write(OBJECT_MAPPER.writeValueAsString(keys));
            } catch (final IOException e) {
                LoggerUtilities.error("Failed to write the keys file!");
                LoggerUtilities.error(e);
            }
        }
    }
}
