package me.chayapak1.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HashingPlugin {
    private final Bot bot;

    public HashingPlugin (Bot bot) {
        this.bot = bot;
    }

    public String getHash (String prefix, PlayerEntry sender, boolean sectionSigns) { return getGenericHash(bot.config.keys.trustedKey, prefix, sender, sectionSigns); }

    public String getAdminHash (String prefix, PlayerEntry sender, boolean sectionSigns) { return getGenericHash(bot.config.keys.adminKey, prefix, sender, sectionSigns); }

    public String getOwnerHash (String prefix, PlayerEntry sender, boolean sectionSigns) { return getGenericHash(bot.config.keys.ownerKey, prefix, sender, sectionSigns); }

    // should this be public?
    public String getGenericHash (String key, String prefix, PlayerEntry sender, boolean sectionSigns) {
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

    private boolean checkHash (String hash, String input) {
        // removes reset section sign
        if (input.length() == (16 * 2 /* <-- don't forget, we have the section signs */) + 2 && input.endsWith("§r"))
            input = input.substring(0, input.length() - 2);

        return input.equals(hash);
    }

    public boolean isCorrectHash (String input, String prefix, PlayerEntry sender) {
        return checkHash(getHash(prefix, sender, true), input) ||
                checkHash(getHash(prefix, sender, false), input);
    }

    public boolean isCorrectAdminHash (String input, String prefix, PlayerEntry sender) {
        return checkHash(getAdminHash(prefix, sender, true), input) ||
                checkHash(getAdminHash(prefix, sender, false), input);
    }

    public boolean isCorrectOwnerHash (String input, String prefix, PlayerEntry sender) {
        return checkHash(getOwnerHash(prefix, sender, true), input) ||
                checkHash(getOwnerHash(prefix, sender, false), input);
    }

    public TrustLevel getTrustLevel (String input, String prefix, PlayerEntry sender) {
        if (isCorrectOwnerHash(input, prefix, sender)) return TrustLevel.OWNER;
        else if (isCorrectAdminHash(input, prefix, sender)) return TrustLevel.ADMIN;
        else if (isCorrectHash(input, prefix, sender)) return TrustLevel.TRUSTED;
        else return TrustLevel.PUBLIC;
    }
}
