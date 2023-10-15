package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.PersistentDataUtilities;

import java.time.Instant;

public class PlayersPersistentDataPlugin extends PlayersPlugin.Listener {
    public static JsonObject playersObject = new JsonObject();

    static {
        if (PersistentDataUtilities.jsonObject.has("players")) {
            playersObject = PersistentDataUtilities.jsonObject.get("players").getAsJsonObject();
        }
    }

    private final Bot bot;

    public PlayersPersistentDataPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (playersObject.has(getName(target))) return;

        final JsonObject object = new JsonObject();
        object.addProperty("uuid", target.profile.getIdAsString());
        object.add("lastSeen", new JsonObject());

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
