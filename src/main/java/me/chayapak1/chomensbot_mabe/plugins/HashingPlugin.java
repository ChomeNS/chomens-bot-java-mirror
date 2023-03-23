package me.chayapak1.chomensbot_mabe.plugins;

import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;
import me.chayapak1.chomensbot_mabe.util.Hexadecimal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

public class HashingPlugin {
    private final Bot bot;

    @Getter private String hash;
    @Getter private String ownerHash;

    public HashingPlugin (Bot bot) {
        this.bot = bot;

        bot.executor().scheduleAtFixedRate(this::update, 1000 * 2, 500, TimeUnit.MILLISECONDS);
    }

    public void update () {
        final String normalHashKey = bot.config().keys().get("normalKey");
        final String ownerHashKey = bot.config().keys().get("ownerKey");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String time = String.valueOf(System.currentTimeMillis() / 10000);

            // messy
            String normalHashInput = time + normalHashKey;
            byte[] normalHashByteHash = md.digest(normalHashInput.getBytes(StandardCharsets.UTF_8));
            hash = Hexadecimal.encode(normalHashByteHash).substring(0, 16);

            String ownerHashInput = time + ownerHashKey;
            byte[] ownerHashByteHash = md.digest(ownerHashInput.getBytes(StandardCharsets.UTF_8));
            ownerHash = Hexadecimal.encode(ownerHashByteHash).substring(0, 16);

            bot.logger().log("normal hash input " + normalHashInput + " owner " + ownerHashInput);
            bot.logger().log(hash + " " + ownerHash);
        } catch (NoSuchAlgorithmException ignored) {}
    }
}
