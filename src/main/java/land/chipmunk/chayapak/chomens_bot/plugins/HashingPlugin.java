package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HashingPlugin {
    private final Bot bot;

    public HashingPlugin (Bot bot) {
        this.bot = bot;
    }

    public String getHash (String prefix, PlayerEntry sender, boolean sectionSigns) {
        final long time = System.currentTimeMillis() / 5_000;

        final String key = bot.config.keys.normalKey;

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

    public String getOwnerHash (String prefix, PlayerEntry sender, boolean sectionSigns) {
        final long time = System.currentTimeMillis() / 5_000;

        final String key = bot.config.keys.ownerKey;

        final String value = sender.profile.getIdAsString() + prefix + time + key;

        final String hash = Hashing.sha256()
                .hashString(value, StandardCharsets.UTF_8)
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
}
