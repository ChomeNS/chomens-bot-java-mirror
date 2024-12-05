package me.chayapak1.chomens_bot.plugins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class IPFilterPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public static ArrayNode filteredIPs = (ArrayNode) PersistentDataUtilities.getOrDefault("ipFilters", JsonNodeFactory.instance.arrayNode());

    public IPFilterPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);

        bot.executor.scheduleAtFixedRate(this::checkAllPlayers, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (filteredIPs.isEmpty()) return;

        check(target);
    }

    private void check (PlayerEntry target) {
        if (bot.options.useCorePlaceBlock) return; // it will spam the place block core so i ignored this

        final CompletableFuture<String> future = bot.players.getPlayerIP(target);

        if (future == null) return;

        future.thenApplyAsync(output -> {
            handleIP(output, target);

            return output;
        });
    }

    public void add (String ip) {
        filteredIPs.add(ip);

        PersistentDataUtilities.put("ipFilters", filteredIPs);

        checkAllPlayers();
    }

    private void checkAllPlayers () {
        if (filteredIPs.isEmpty()) return;

        bot.executorService.submit(() -> {
            for (PlayerEntry entry : bot.players.list) check(entry);
        });
    }

    public String remove (int index) {
        final JsonNode element = filteredIPs.remove(index);

        PersistentDataUtilities.put("ipFilters", filteredIPs);

        return element.asText();
    }

    public void clear () {
        while (!filteredIPs.isEmpty()) filteredIPs.remove(0);

        PersistentDataUtilities.put("ipFilters", filteredIPs);
    }

    private void handleIP (String ip, PlayerEntry entry) {
        for (JsonNode element : filteredIPs.deepCopy()) {
            if (!element.asText().equals(ip)) continue;

            if (entry.profile.getId().equals(bot.profile.getId())) continue;

            bot.filter.doAll(entry);
        }
    }
}
