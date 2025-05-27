package me.chayapak1.chomens_bot.evalFunctions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.SNBTUtilities;

import java.util.List;

public class GetPlayerListFunction extends EvalFunction {
    public GetPlayerListFunction () {
        super("getPlayerList");
    }

    @Override
    public Output execute (final Bot bot, final Object... args) {
        final List<PlayerEntry> list = bot.players.list;

        final JsonArray array = new JsonArray();

        for (final PlayerEntry entry : list) {
            final JsonObject object = new JsonObject();

            object.addProperty("uuid", entry.profile.getIdAsString());
            object.addProperty("username", entry.profile.getName());
            if (entry.displayName != null)
                object.addProperty("displayName", SNBTUtilities.fromComponent(false, entry.displayName));

            array.add(object);
        }

        return new Output(array.toString(), true);
    }
}
