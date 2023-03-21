package me.chayapak1.chomensbot_mabe.plugins;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class HashingPlugin {
    private final Bot bot;

    @Getter private String hash;
    @Getter private String ownerHash;

    public HashingPlugin (Bot bot) {
        this.bot = bot;
        bot.executor().schedule(this::update, 2, TimeUnit.SECONDS);
    }

    public void update () {
        // final String ownerHashKey = "b)R��nF�CW���#�\\[�S*8\"t^eia�Z��k����K1�8zȢ�";
        // final String normalHashKey = "�iB_D���k��j8H�{?[/ڭ�f�}Ѣ�^-=�Ț��v]��g>��=c";

        final String normalHashKey = bot.keys().get("normalKey");
        final String ownerHashKey = bot.keys().get("ownerKey");

        final String hashValue = System.currentTimeMillis() / 10_000 + normalHashKey;
        hash = Hashing.sha256()
                .hashString(hashValue, StandardCharsets.UTF_8)
                .toString().substring(0, 16);

        final String ownerHashValue = System.currentTimeMillis() / 10_000 + ownerHashKey;
        ownerHash = Hashing.sha256()
                .hashString(ownerHashValue, StandardCharsets.UTF_8)
                .toString().substring(0, 16);
    }
}
