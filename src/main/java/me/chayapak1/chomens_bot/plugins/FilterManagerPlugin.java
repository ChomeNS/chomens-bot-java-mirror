package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FilterManagerPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public final Map<PlayerEntry, String> list = Collections.synchronizedMap(new HashMap<>());

    public FilterManagerPlugin (Bot bot) {
        this.bot = bot;

        bot.players.addListener(this);

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public boolean playerMessageReceived(PlayerMessage message) {
                FilterManagerPlugin.this.playerMessageReceived(message);

                return true;
            }
        });

        bot.commandSpy.addListener(new CommandSpyPlugin.Listener() {
            @Override
            public void commandReceived(PlayerEntry sender, String command) {
                FilterManagerPlugin.this.commandSpyMessageReceived(sender, command);
            }
        });

        bot.executor.scheduleAtFixedRate(this::kick, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void playerLeft (PlayerEntry target) {
        remove(target.profile.getName());
    }

    @Override
    public void playerDisplayNameUpdated (PlayerEntry target, Component displayName) {
        final Pair<PlayerEntry, String> player = getFilteredFromName(target.profile.getName());

        if (player == null) return;

        // we use the stringified instead of the component because you can configure the OP and DeOP tag in
        // the extras config
        final String stringifiedDisplayName = ComponentUtilities.stringify(displayName);

        if (stringifiedDisplayName.startsWith("[OP] ")) deOp(target);
    }

    public void commandSpyMessageReceived (PlayerEntry entry, String command) {
        final Pair<PlayerEntry, String> player = getFilteredFromName(entry.profile.getName());

        if (player == null) return;

        if (
                command.startsWith("/mute") ||
                        command.startsWith("/emute") ||
                        command.startsWith("/silence") ||
                        command.startsWith("/esilence") ||
                        command.startsWith("/essentials:mute") ||
                        command.startsWith("/essentials:emute") ||
                        command.startsWith("/essentials:silence") ||
                        command.startsWith("/essentials:esilence")
        ) mute(entry, player.getRight());

        deOp(entry);
        gameMode(entry);
        bot.exploits.kick(entry.profile.getId());
    }

    public void playerMessageReceived (PlayerMessage message) {
        if (message.sender.profile.getName() == null) return;

        final Pair<PlayerEntry, String> player = getFilteredFromName(message.sender.profile.getName());

        if (player == null || message.sender.profile.getId().equals(new UUID(0L, 0L))) return;

        doAll(message.sender, player.getRight());
    }

    public void doAll (PlayerEntry entry) { doAll(entry, ""); }
    public void doAll (PlayerEntry entry, String reason) {
        mute(entry, reason);
        deOp(entry);
        gameMode(entry);
        bot.exploits.kick(entry.profile.getId());
    }

    public void mute (PlayerEntry target) { mute(target, ""); }
    public void mute (PlayerEntry target, String reason) {
        bot.core.run("essentials:mute " + target.profile.getIdAsString() + " 10y " + reason);
    }

    public void deOp (PlayerEntry target) {
        bot.core.run("minecraft:execute run deop " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void gameMode (PlayerEntry target) {
        bot.core.run("minecraft:gamemode adventure " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void kick () {
        for (PlayerEntry filtered : list.keySet()) {
            bot.exploits.kick(filtered.profile.getId());
        }
    }

    public void add (PlayerEntry entry, String reason) {
        if (
                getFilteredFromName(entry.profile.getName()) != null || // prevent already existing filters
                entry.profile.equals(bot.profile) // prevent self-harm !!!!!!
        ) return;

        list.put(entry, reason);

        doAll(entry, reason);
    }

    public void remove (String name) {
        list.entrySet().removeIf(filtered -> filtered.getKey().profile.getName().equals(name));
    }

    public Pair<PlayerEntry, String> getFilteredFromName (String name) {
        for (Map.Entry<PlayerEntry, String> entry : list.entrySet()) {
            if (entry.getKey().profile.getName().equals(name)) return Pair.of(entry.getKey(), entry.getValue());
        }

        return null;
    }
}
