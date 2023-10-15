package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.PersistentDataUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.*;
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

        bot.executor.scheduleAtFixedRate(this::checkAllPlayers, 0, 5, TimeUnit.SECONDS);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (filteredIPs.isEmpty()) return;

        check(target);
    }

    private void check (PlayerEntry target) {
        final CompletableFuture<CompoundTag> future = bot.core.runTracked("essentials:seen " + target.profile.getIdAsString());

        if (future == null) return;

        future.thenApply(tags -> {
            if (!tags.contains("LastOutput") || !(tags.get("LastOutput") instanceof StringTag)) return tags;

            final StringTag lastOutput = tags.get("LastOutput");

            final Component output = GsonComponentSerializer.gson().deserialize(lastOutput.getValue());

            final List<Component> children = output.children();

            String stringified = ComponentUtilities.stringify(Component.join(JoinConfiguration.separator(Component.space()), children));

            if (!stringified.startsWith(" - IP Address: ")) return tags;

            stringified = stringified.substring(" - IP Address: ".length());
            if (stringified.startsWith("/")) stringified = stringified.substring(1);

            handleIP(stringified, target);

            return tags;
        });
    }

    public void add (String ip) {
        filteredIPs.add(ip);

        PersistentDataUtilities.put("ipFilters", filteredIPs);

        checkAllPlayers();
    }

    private void checkAllPlayers () {
        if (filteredIPs.isEmpty()) return;

        int ms = 0;
        for (PlayerEntry entry : bot.players.list) {
            bot.executor.schedule(() -> check(entry), ms, TimeUnit.MILLISECONDS);

            ms += 350;
        }
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
