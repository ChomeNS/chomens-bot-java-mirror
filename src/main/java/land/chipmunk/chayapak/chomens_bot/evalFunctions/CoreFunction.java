package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import land.chipmunk.chayapak.chomens_bot.Bot;

public class CoreFunction extends EvalFunction {
    public CoreFunction (Bot bot) {
        super("core", bot);
    }

    @Override
    public void execute(Object... args) {
        final String command = (String) args[0];

        bot.core.run(command);
    }
}
