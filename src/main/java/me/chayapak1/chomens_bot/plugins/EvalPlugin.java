package me.chayapak1.chomens_bot.plugins;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.socket.client.IO;
import io.socket.client.Socket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.eval.EvalFunction;
import me.chayapak1.chomens_bot.data.eval.EvalOutput;
import me.chayapak1.chomens_bot.evalFunctions.*;
import me.chayapak1.chomens_bot.util.LoggerUtilities;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class EvalPlugin {
    // we need another executor service to not interfere with the main one (in case people spam the bridge)
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(
            2,
            new ThreadFactoryBuilder()
                    .setNameFormat("ExecutorService (eval)")
                    .build()
    );

    private static final String BRIDGE_PREFIX = "function:";
    public static final List<EvalFunction> FUNCTIONS = ObjectList.of(
            new CoreFunction(),
            new CorePlaceBlockFunction(),
            new ChatFunction(),
            new GetPlayerListFunction(),
            new GetBotInfoFunction()
    );
    private static final Gson GSON = new Gson();

    private static Socket socket = null;
    public static boolean connected = false;
    private static boolean initialized = false;

    private static final AtomicInteger transactionId = new AtomicInteger();
    private static final Map<Integer, CompletableFuture<EvalOutput>> futures = new Object2ObjectOpenHashMap<>();

    public static void connect (final String address) {
        try {
            socket = IO.socket(address);
        } catch (final Exception e) {
            LoggerUtilities.error(e);
            return;
        }

        final JsonArray functionsArray = new JsonArray();

        for (final Bot bot : Main.bots) {
            final String server = bot.getServerString(true);

            for (final EvalFunction function : FUNCTIONS) {
                final JsonObject object = new JsonObject();

                object.addProperty("name", function.name);
                object.addProperty("server", server);

                functionsArray.add(object);
            }
        }

        socket.on("codeOutput", outputArgs -> {
            if (outputArgs.length < 3) return;

            try {
                final int id = (int) outputArgs[0];
                final boolean isError = (boolean) outputArgs[1];
                final String output = (String) outputArgs[2];

                if (!futures.containsKey(id)) return;

                final CompletableFuture<EvalOutput> future = futures.remove(id);

                future.complete(new EvalOutput(isError, output));
            } catch (final ClassCastException | NumberFormatException ignored) { }
        });

        socket.on(Socket.EVENT_CONNECT, args -> {
            connected = true;

            socket.emit("setFunctions", GSON.toJson(functionsArray));

            if (initialized) return;
            else initialized = true;

            for (final Bot bot : Main.bots) {
                for (final EvalFunction function : FUNCTIONS) {
                    socket.on(
                            BRIDGE_PREFIX + function.name + ":" + bot.getServerString(true),
                            functionArgs -> {
                                final ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) EXECUTOR_SERVICE;

                                // ignore the bridge request from the eval server if we have too much
                                if (threadPoolExecutor.getQueue().size() > 75) return;

                                EXECUTOR_SERVICE.execute(() -> {
                                    try {
                                        final EvalFunction.Output output = function.execute(bot, functionArgs);
                                        if (output == null) return;
                                        socket.emit("functionOutput:" + function.name, output.message(), output.parseJSON());
                                    } catch (final Exception ignored) { }
                                });
                            }
                    );
                }
            }
        });
        socket.on(Socket.EVENT_DISCONNECT, args -> connected = false);
        socket.on(Socket.EVENT_CONNECT_ERROR, args -> connected = false);

        socket.connect();
    }

    private final Bot bot;

    public EvalPlugin (final Bot bot) {
        this.bot = bot;
    }

    public CompletableFuture<EvalOutput> run (final String code) {
        final CompletableFuture<EvalOutput> future = new CompletableFuture<>();

        if (!connected) return null;

        socket.emit("runCode", bot.getServerString(true), transactionId.get(), code);

        futures.put(transactionId.getAndIncrement(), future);

        return future;
    }

    public static void reset () {
        if (!connected) return;
        socket.emit("reset");
    }
}
