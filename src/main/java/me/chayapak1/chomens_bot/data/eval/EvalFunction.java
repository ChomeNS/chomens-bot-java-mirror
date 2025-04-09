package me.chayapak1.chomens_bot.data.eval;

import me.chayapak1.chomens_bot.Bot;

public class EvalFunction {
    public final String name;

    protected final Bot bot;

    public EvalFunction (
            final String name,
            final Bot bot
    ) {
        this.name = name;
        this.bot = bot;
    }

    public Output execute (final Object... args) throws Exception { return null; }

    public record Output(String message, boolean parseJSON) {
    }
}
