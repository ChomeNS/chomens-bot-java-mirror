package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;

import java.util.ArrayList;
import java.util.List;

public class WhitelistPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public final List<String> list = new ArrayList<>();

    private boolean enabled = false;

    public WhitelistPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);
    }

    public void enable () {
        enabled = true;

        for (PlayerEntry entry : bot.players.list) {
            if (list.contains(entry.profile.getName())) continue;

            list.add(entry.profile.getName());

            bot.filterManager.remove(entry.profile.getName());
        }
    }

    public void disable () {
        enabled = false;

        for (PlayerEntry entry : bot.players.list) {
            bot.filterManager.remove(entry.profile.getName());
        }
    }

    public void add (String player) {
        list.add(player);

        bot.filterManager.remove(player);
    }

    public String remove (int index) {
        final String removed = list.remove(index);

        checkAndAddToFilterManager(removed);

        return removed;
    }

    public void clear () {
        list.removeIf(eachPlayer -> !eachPlayer.equals(bot.profile.getName()));

        for (PlayerEntry entry : bot.players.list) {
            if (entry.profile.equals(bot.profile)) continue;

            bot.filterManager.add(entry, "");
        }
    }

    public boolean isBlacklisted (String name) { return !list.contains(name); }

    private void checkAndAddToFilterManager (String player) {
        final PlayerEntry entry = bot.players.getEntry(player);

        if (entry == null) return;

        bot.filterManager.add(entry, "");
    }

    @Override
    public void playerJoined (PlayerEntry target) {
        if (!enabled) return;

        if (isBlacklisted(target.profile.getName())) bot.filterManager.add(target, "");
    }
}
