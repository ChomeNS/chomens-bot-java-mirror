package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;

public class CoreFunction extends EvalFunction {
    public CoreFunction (Bot bot) {
        super("core", bot);
    }

    @Override
    public Output execute(Object... args) {
        if (args.length == 0) return null;

        final String command = (String) args[0];

        bot.core.run(command);

        return null;
    }
}
