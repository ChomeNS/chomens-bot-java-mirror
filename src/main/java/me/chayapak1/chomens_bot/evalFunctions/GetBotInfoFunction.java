package me.chayapak1.chomens_bot.evalFunctions;

import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;

public class GetBotInfoFunction extends EvalFunction {
    public GetBotInfoFunction () {
        super("getBotInfo");
    }

    @Override
    public Output execute (final Bot bot, final Object... args) {
        final JsonObject object = new JsonObject();

        object.addProperty("username", bot.username);
        object.addProperty("host", bot.host);
        object.addProperty("port", bot.port);
        object.addProperty("loggedIn", bot.loggedIn);

        return new Output(object.toString(), true);
    }
}
