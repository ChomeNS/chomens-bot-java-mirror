package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;

public class GetBotUsernameFunction extends EvalFunction {
    public GetBotUsernameFunction (Bot bot) {
        super("getBotUsername", bot);
    }

    @Override
    public Output execute(Object... args) {
        return new Output(bot.username, false);
    }
}
