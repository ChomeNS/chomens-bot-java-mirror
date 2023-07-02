package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.EvalOutput;

public class EvalFunction {
    public final String name;

    protected final Bot bot;

    public EvalFunction (
            String name,
            Bot bot
    ) {
        this.name = name;
        this.bot = bot;
    }

    public void execute (Object ...args) {}
}
