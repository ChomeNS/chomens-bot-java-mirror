package land.chipmunk.chayapak.chomens_bot.evalFunctions;

import land.chipmunk.chayapak.chomens_bot.Bot;

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

    public Output execute (Object ...args) { return null; }

    public static class Output {
        public final String message;
        public final boolean parseJSON;

        public Output (String message, boolean parseJSON) {
            this.message = message;
            this.parseJSON = parseJSON;
        }
    }
}
