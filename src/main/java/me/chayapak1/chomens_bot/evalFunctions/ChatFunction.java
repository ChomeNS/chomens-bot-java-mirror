package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;

public class ChatFunction extends EvalFunction {
    public ChatFunction () {
        super("chat");
    }

    @Override
    public Output execute (final Bot bot, final Object... args) {
        if (args.length == 0) return null;

        final String message = (String) args[0];

        bot.chat.send(message);

        return null;
    }
}
