package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class IPFilterPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public static JsonArray filteredIPs = new JsonArray();

    static {
        if (PersistentDataUtilities.jsonObject.has("ipFilters")) {
            filteredIPs = PersistentDataUtilities.jsonObject.get("ipFilters").getAsJsonArray();
        }
    }

    public IPFilterPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);

        bot.executor.scheduleAtFixedRate(this::checkAllPlayers, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (filteredIPs.isEmpty()) return;

        check(target);
    }

    private void check (PlayerEntry target) {
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

        for (PlayerEntry entry : bot.players.list) check(entry);
    }

    public String remove (int index) {
        final JsonElement element = filteredIPs.remove(index);

        PersistentDataUtilities.put("ipFilters", filteredIPs);

        return element.getAsString();
    }

    public void clear () {
        while (!filteredIPs.isEmpty()) filteredIPs.remove(0);

        PersistentDataUtilities.put("ipFilters", filteredIPs);
    }

    private void handleIP (String ip, PlayerEntry entry) {
        for (JsonElement element : filteredIPs) {
            if (!element.getAsString().equals(ip)) continue;

            if (entry.profile.getId().equals(bot.profile.getId())) continue;

            bot.filter.doAll(entry);
        }
    }
}
