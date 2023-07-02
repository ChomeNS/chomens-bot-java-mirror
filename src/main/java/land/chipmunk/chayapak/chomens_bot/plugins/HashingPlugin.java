package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.nio.charset.StandardCharsets;

public class HashingPlugin {
    private final Bot bot;

    public String hash;
    public String ownerHash;

    private long lastTime;

    public HashingPlugin (Bot bot) {
        this.bot = bot;

        bot.tick.addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                update();
            }
        });
    }

    public void update () {
        final long time = System.currentTimeMillis() / 5_000;

        // mabe this will optimize it?
        if (time == lastTime) return;
        lastTime = time;

        final String normalHashKey = bot.config.keys.normalKey;
        final String ownerHashKey = bot.config.keys.ownerKey;

        final String hashValue = time + normalHashKey;
        hash = Hashing.sha256()
                .hashString(hashValue, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 16);

        final String ownerHashValue = (System.currentTimeMillis() / 5_000) + ownerHashKey;
        ownerHash = Hashing.sha256()
                .hashString(ownerHashValue, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 16);
    }
}
