package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import io.socket.client.IO;
import io.socket.client.Socket;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.EvalOutput;
import land.chipmunk.chayapak.chomens_bot.evalFunctions.ChatFunction;
import land.chipmunk.chayapak.chomens_bot.evalFunctions.CoreFunction;
import land.chipmunk.chayapak.chomens_bot.evalFunctions.EvalFunction;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class EvalPlugin {
    public static final String BRIDGE_PREFIX = "function:";

    public boolean connected = false;

    private Socket socket = null;

    private int transactionId = 0;

    private final Map<Integer, CompletableFuture<EvalOutput>> futures = new HashMap<>();

    public final List<EvalFunction> functions = new ArrayList<>();

    private final Gson gson = new Gson();

    public EvalPlugin (Bot bot) {
        functions.add(new CoreFunction(bot));
        functions.add(new ChatFunction(bot));

        try {
            socket = IO.socket(bot.config.eval.address);
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, (args) -> {
            connected = true;

            final JsonArray array = new JsonArray();

            for (EvalFunction function : functions) array.add(function.name);

            socket.emit(
                    "setFunctions",
                    gson.toJson(array)
            );
        });
        socket.on(Socket.EVENT_DISCONNECT, (args) -> connected = false);
        socket.on(Socket.EVENT_CONNECT_ERROR, (args) -> connected = false);

        for (EvalFunction function : functions) socket.on(BRIDGE_PREFIX + function.name, function::execute);

        socket.on("codeOutput", (args) -> {
            final int id = (int) args[0];
            final boolean isError = (boolean) args[1];
            final String output = (String) args[2];

            final CompletableFuture<EvalOutput> future = futures.get(id);

            future.complete(new EvalOutput(isError, output));
        });

        socket.connect();
    }

    public CompletableFuture<EvalOutput> run (String code) {
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
