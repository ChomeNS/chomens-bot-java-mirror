package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.List;

public class GetPlayerListFunction extends EvalFunction {
    public GetPlayerListFunction(Bot bot) {
        super("getPlayerList", bot);
    }

    @Override
    public Output execute(Object... args) {
        final List<PlayerEntry> list = bot.players.list;

        final JsonArray array = new JsonArray();

        for (PlayerEntry entry : list) {
            final JsonObject object = new JsonObject();

            object.addProperty("uuid", entry.profile.getIdAsString());
            object.addProperty("username", entry.profile.getName());
            if (entry.displayName != null) object.addProperty("displayName", GsonComponentSerializer.gson().serialize(entry.displayName));

            array.add(object);
        }

        return new Output(array.toString(), true);
    }
}
