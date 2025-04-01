package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CoreFunction extends EvalFunction {
    private long lastExecutionTime = System.currentTimeMillis();

    public CoreFunction (Bot bot) {
        super("core", bot);
    }

    @Override
    public Output execute (Object... args) throws Exception {
        if (args.length == 0) return null;

        // prevent 69 DDOS exploit !!!
        if (System.currentTimeMillis() - lastExecutionTime < 50) return null;
        lastExecutionTime = System.currentTimeMillis();

        final String command = (String) args[0];

        final CompletableFuture<Component> future = bot.core.runTracked(command);

        return new Output(GsonComponentSerializer.gson().serialize(future.get(1, TimeUnit.SECONDS)), true);
    }
}
