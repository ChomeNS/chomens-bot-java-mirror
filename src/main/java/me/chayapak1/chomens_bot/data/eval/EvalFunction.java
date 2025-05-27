package me.chayapak1.chomens_bot.data.eval;

import me.chayapak1.chomens_bot.Bot;

public class EvalFunction {
    public final String name;

    public EvalFunction (final String name) {
        this.name = name;
    }

    public Output execute (final Bot bot, final Object... args) throws Exception { return null; }

    public record Output(String message, boolean parseJSON) {
    }
}
