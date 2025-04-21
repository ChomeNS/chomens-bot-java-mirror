package me.chayapak1.chomens_bot.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.socket.client.IO;
import io.socket.client.Socket;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.data.eval.EvalOutput;
import me.chayapak1.chomens_bot.evalFunctions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EvalPlugin {
    public static final String BRIDGE_PREFIX = "function:";

    public boolean connected = false;

    private Socket socket = null;

    private int transactionId = 0;

    private final Map<Integer, CompletableFuture<EvalOutput>> futures = new HashMap<>();

    public final List<EvalFunction> functions = new ArrayList<>();

    private final Gson gson = new Gson();

    public EvalPlugin (final Bot bot) {
        functions.add(new CoreFunction(bot));
        functions.add(new CorePlaceBlockFunction(bot));
        functions.add(new ChatFunction(bot));
        functions.add(new GetPlayerListFunction(bot));
        functions.add(new GetBotInfoFunction(bot));
        functions.add(new GetLatestChatMessageFunction(bot));

        try {
            socket = IO.socket(bot.config.eval.address);
        } catch (final Exception e) {
            bot.logger.error(e);
        }

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            connected = true;

            final JsonArray array = new JsonArray();

            for (final EvalFunction function : functions) array.add(function.name);

            socket.emit(
                    "setFunctions",
                    gson.toJson(array)
            );
        });
        socket.on(Socket.EVENT_DISCONNECT, (args) -> connected = false);
        socket.on(Socket.EVENT_CONNECT_ERROR, (args) -> connected = false);

        for (final EvalFunction function : functions) {
            socket.on(
                    BRIDGE_PREFIX + function.name,
                    args ->
                            bot.executorService.submit(() -> {
                                try {
                                    final EvalFunction.Output output = function.execute(args);

                                    if (output == null) return;

                                    socket.emit("functionOutput:" + function.name, output.message(), output.parseJSON());
                                } catch (final Exception ignored) { }
                            })
            );
        }

        socket.on("codeOutput", (args) -> {
            if (args.length < 3) return;

            try {
                final int id = (int) args[0];
                final boolean isError = (boolean) args[1];
                final String output = (String) args[2];

                if (!futures.containsKey(id)) return;

                final CompletableFuture<EvalOutput> future = futures.remove(id);

                future.complete(new EvalOutput(isError, output));
            } catch (final NumberFormatException ignored) { }
        });

        socket.connect();
    }

    public CompletableFuture<EvalOutput> run (final String code) {
        final CompletableFuture<EvalOutput> future = new CompletableFuture<>();

        if (!connected) return null;

        socket.emit("runCode", transactionId, code);

        futures.put(transactionId, future);

        transactionId++;

        return future;
    }

    public void reset () {
        if (!connected) return;

        socket.emit("reset");
    }
}
