package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class PlayersPersistentDataPlugin extends PlayersPlugin.Listener {
    public static ObjectNode playersObject = (ObjectNode) PersistentDataUtilities.getOrDefault("players", JsonNodeFactory.instance.objectNode());

    public static final ReentrantLock lock = new ReentrantLock();

    private final Bot bot;

    public PlayersPersistentDataPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    @Override
    public synchronized void playerJoined(PlayerEntry target) {
        bot.executorService.submit(() -> {
            try {
                lock.lock();

                final JsonNode originalElement = playersObject.get(target.profile.getName());

                ObjectNode object;

                if (originalElement == null || originalElement.isNull()) {
                    object = JsonNodeFactory.instance.objectNode();
                    object.put("uuid", target.profile.getIdAsString());
                    object.set("ips", JsonNodeFactory.instance.objectNode());
                } else if (originalElement instanceof ObjectNode) {
                    object = (ObjectNode) originalElement;
                } else {
                    lock.unlock();
                    return;
                }

                lock.unlock();

                final CompletableFuture<String> future = bot.players.getPlayerIP(target);

                if (future == null) {
                    setPersistentEntry(target, object);
                    return;
                }

                future.completeOnTimeout(null, 5, TimeUnit.SECONDS);

                future.thenApplyAsync(output -> {
                    if (output != null) {
                        ((ObjectNode) object.get("ips")).put(bot.host + ":" + bot.port, output);
                    }

                    setPersistentEntry(target, object);

                    return output;
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // is this bad?
    private synchronized void setPersistentEntry (PlayerEntry target, ObjectNode object) {
        try {
            lock.lock();

            playersObject.set(getName(target), object);

            PersistentDataUtilities.put("players", playersObject);

            lock.unlock();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getName (PlayerEntry target) {
        return bot.options.creayun ? target.profile.getName().replaceAll("§.", "") : target.profile.getName();
    }

    @Override
    public synchronized void playerLeft(PlayerEntry target) {
        lock.lock();

        if (!playersObject.has(getName(target))) return;

        final ObjectNode player = (ObjectNode) playersObject.get(getName(target));

        final ObjectNode object = JsonNodeFactory.instance.objectNode();
        object.put("time", Instant.now().toEpochMilli());
        object.put("server", bot.host + ":" + bot.port);

        player.set("lastSeen", object);

        PersistentDataUtilities.put("players", playersObject);

        lock.unlock();
    }
}
