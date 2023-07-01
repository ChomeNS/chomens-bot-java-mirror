package land.chipmunk.chayapak.chomens_bot.plugins;

import io.socket.client.IO;
import io.socket.client.Socket;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.EvalOutput;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EvalPlugin {
    public static final String BRIDGE_PREFIX = "function:";

    @Getter private boolean connected = false;

    private Socket socket = null;

    private int transactionId = 0;

    private final Map<Integer, CompletableFuture<EvalOutput>> futures = new HashMap<>();

    public EvalPlugin (Bot bot) {
        try {
            socket = IO.socket(bot.config().eval().address());
        } catch (Exception e) {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, (args) -> connected = true);
        socket.on(Socket.EVENT_DISCONNECT, (args) -> connected = false);
        socket.on(Socket.EVENT_CONNECT_ERROR, (args) -> connected = false);

        socket.on("codeOutput", (args) -> {
            final int id = (int) args[0];
            final boolean isError = (boolean) args[1];
            final String output = (String) args[2];

            final CompletableFuture<EvalOutput> future = futures.get(id);

            future.complete(new EvalOutput(isError, output));
        });

        socket.on(BRIDGE_PREFIX + "chat", (args) -> {
            final String message = (String) args[0];

            bot.chat().send(message);
        });

        socket.on(BRIDGE_PREFIX + "core", (args) -> {
            final String command = (String) args[0];

            bot.core().run(command);
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
