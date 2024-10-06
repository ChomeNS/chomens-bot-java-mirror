package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.PersistentDataUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.List;
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
        final CompletableFuture<Component> future = bot.core.runTracked("essentials:seen " + target.profile.getIdAsString());

        if (future == null) return;

        future.thenApply(output -> {
            final List<Component> children = output.children();

            String stringified = ComponentUtilities.stringify(Component.join(JoinConfiguration.separator(Component.space()), children));

            if (!stringified.startsWith(" - IP Address: ")) return output;

            stringified = stringified.trim().substring(" - IP Address: ".length());
            if (stringified.startsWith("/")) stringified = stringified.substring(1);

            handleIP(stringified, target);

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
