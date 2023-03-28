package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import land.chipmunk.chayapak.chomens_bot.Bot;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HashingPlugin {
    private final Bot bot;

    @Getter private String hash;
    @Getter private String ownerHash;

    public HashingPlugin (Bot bot) {
        this.bot = bot;

        bot.executor().scheduleAtFixedRate(this::update, 0, 1, TimeUnit.SECONDS);
    }

    public void update () {
        final String normalHashKey = bot.config().keys().get("normalKey");
        final String ownerHashKey = bot.config().keys().get("ownerKey");

        final String hashValue = (System.currentTimeMillis() / 10_000) + normalHashKey;
        hash = Hashing.sha256()
                .hashString(hashValue, StandardCharsets.UTF_8)
                .toString().substring(0, 16);

        final String ownerHashValue = (System.currentTimeMillis() / 10_000) + ownerHashKey;
        ownerHash = Hashing.sha256()
                .hashString(ownerHashValue, StandardCharsets.UTF_8)
                .toString().substring(0, 16);
    }
}
