package me.chayapak1.chomens_bot.evalFunctions;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.util.SNBTUtilities;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CoreFunction extends EvalFunction {
    private long lastExecutionTime = System.currentTimeMillis();

    public CoreFunction () {
        super("core");
    }

    @Override
    public Output execute (final Bot bot, final Object... args) throws Exception {
        if (args.length == 0) return null;

        // prevent 69 DDOS exploit !!!
        if (System.currentTimeMillis() - lastExecutionTime < 50) return null;
        lastExecutionTime = System.currentTimeMillis();

        final String command = (String) args[0];

        final CompletableFuture<Component> future = bot.core.runTracked(command);

        return new Output(
                SNBTUtilities.fromComponent(
                        false,
                        future.get(1, TimeUnit.SECONDS)
                ),
                true
        );
    }
}
