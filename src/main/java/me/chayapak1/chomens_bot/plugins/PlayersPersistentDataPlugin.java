package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayersPersistentDataPlugin extends PlayersPlugin.Listener {
    public static JsonObject playersObject = PersistentDataUtilities.getOrDefault("players", new JsonObject()).getAsJsonObject();

    private final Bot bot;

    public PlayersPersistentDataPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        final JsonElement originalElement = playersObject.get(target.profile.getName());

        JsonObject object;

        if (originalElement == null) {
            object = new JsonObject();
            object.addProperty("uuid", target.profile.getIdAsString());
            object.add("ips", new JsonObject());
        } else {
            object = originalElement.getAsJsonObject();
        }

        final CompletableFuture<String> future = bot.players.getPlayerIP(target);

        if (future == null) {
            setPersistentEntry(target, object);
            return;
        }

        future.completeOnTimeout(null, 5, TimeUnit.SECONDS);

        future.thenApplyAsync(output -> {
            if (output != null) {
                object.getAsJsonObject("ips").addProperty(bot.host + ":" + bot.port, output);
            }

            setPersistentEntry(target, object);

            return output;
        });
    }

    // is this bad?
    private void setPersistentEntry (PlayerEntry target, JsonObject object) {
        playersObject.add(getName(target), object);

        PersistentDataUtilities.put("players", playersObject);
    }

    private String getName(PlayerEntry target) {
        return bot.options.creayun ? target.profile.getName().replaceAll("§.", "") : target.profile.getName();
    }

    @Override
    public void playerLeft(PlayerEntry target) {
        if (!playersObject.has(getName(target))) return;

        final JsonObject player = playersObject.get(getName(target)).getAsJsonObject();

        final JsonObject object = new JsonObject();
        object.addProperty("time", Instant.now().toEpochMilli());
        object.addProperty("server", bot.host + ":" + bot.port);

        player.add("lastSeen", object);

        PersistentDataUtilities.put("players", playersObject);
    }
}
