package me.chayapak1.chomens_bot.data.eval;

import me.chayapak1.chomens_bot.Bot;

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

    public Output execute (Object... args) throws Exception { return null; }

    public static class Output {
        public final String message;
        public final boolean parseJSON;

        public Output (String message, boolean parseJSON) {
            this.message = message;
            this.parseJSON = parseJSON;
        }
    }
}
