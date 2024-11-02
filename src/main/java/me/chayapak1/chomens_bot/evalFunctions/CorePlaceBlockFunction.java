package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;

public class CorePlaceBlockFunction extends EvalFunction {
    public CorePlaceBlockFunction (Bot bot) {
        super("corePlaceBlock", bot);
    }

    @Override
    public Output execute(Object... args) {
        if (args.length == 0) return null;

        final String command = (String) args[0];

        bot.core.runPlaceBlock(command);

        return null;
    }
}
