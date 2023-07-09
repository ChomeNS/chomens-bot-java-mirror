package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import land.chipmunk.chayapak.chomens_bot.Bot;

public class CorePlaceBlockFunction extends EvalFunction {
    public CorePlaceBlockFunction (Bot bot) {
        super("corePlaceBlock", bot);
    }

    @Override
    public void execute(Object... args) {
        final String command = (String) args[0];

        bot.core.runPlaceBlock(command);
    }
}
