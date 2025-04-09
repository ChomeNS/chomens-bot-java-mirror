package me.chayapak1.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HashingPlugin {
    private final Bot bot;

    public HashingPlugin (final Bot bot) {
        this.bot = bot;
    }

    public String getHash (final String prefix, final PlayerEntry sender, final boolean sectionSigns) { return getGenericHash(bot.config.keys.trustedKey, prefix, sender, sectionSigns); }

    public String getAdminHash (final String prefix, final PlayerEntry sender, final boolean sectionSigns) { return getGenericHash(bot.config.keys.adminKey, prefix, sender, sectionSigns); }

    public String getOwnerHash (final String prefix, final PlayerEntry sender, final boolean sectionSigns) { return getGenericHash(bot.config.keys.ownerKey, prefix, sender, sectionSigns); }

    // should this be public?
    public String getGenericHash (final String key, final String prefix, final PlayerEntry sender, final boolean sectionSigns) {
        final long time = System.currentTimeMillis() / 5_000;

        final String hashValue = sender.profile.getIdAsString() + prefix + time + key;

        final String hash = Hashing.sha256()
                .hashString(hashValue, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 16);

        return sectionSigns ?
                String.join("",
                            Arrays.stream(hash.split(""))
                                    .map((letter) -> "§" + letter)
                                    .toArray(String[]::new)
                ) :
                hash;
    }

    private boolean checkHash (final String hash, String input) {
        // removes reset section sign
        if (input.length() == (16 * 2 /* <-- don't forget, we have the section signs */) + 2 && input.endsWith("§r"))
            input = input.substring(0, input.length() - 2);

        return input.equals(hash);
    }

    public boolean isCorrectHash (final String input, final String prefix, final PlayerEntry sender) {
        return checkHash(getHash(prefix, sender, true), input) ||
                checkHash(getHash(prefix, sender, false), input);
    }

    public boolean isCorrectAdminHash (final String input, final String prefix, final PlayerEntry sender) {
        return checkHash(getAdminHash(prefix, sender, true), input) ||
                checkHash(getAdminHash(prefix, sender, false), input);
    }

    public boolean isCorrectOwnerHash (final String input, final String prefix, final PlayerEntry sender) {
        return checkHash(getOwnerHash(prefix, sender, true), input) ||
                checkHash(getOwnerHash(prefix, sender, false), input);
    }

    public TrustLevel getTrustLevel (final String input, final String prefix, final PlayerEntry sender) {
        if (isCorrectOwnerHash(input, prefix, sender)) return TrustLevel.OWNER;
        else if (isCorrectAdminHash(input, prefix, sender)) return TrustLevel.ADMIN;
        else if (isCorrectHash(input, prefix, sender)) return TrustLevel.TRUSTED;
        else return TrustLevel.PUBLIC;
    }
}
