package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FilterManagerPlugin
        extends Bot.Listener
        implements PlayersPlugin.Listener, ChatPlugin.Listener, CommandSpyPlugin.Listener, TickPlugin.Listener {
    private final Bot bot;

    public final Map<PlayerEntry, String> list = Collections.synchronizedMap(new HashMap<>());

    public FilterManagerPlugin (final Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::kick, 0, 10, TimeUnit.SECONDS);

        bot.addListener(this);
        bot.players.addListener(this);
        bot.tick.addListener(this);
        bot.chat.addListener(this);
        bot.commandSpy.addListener(this);
    }

    @Override
    public void onSecondTick () {
        removeLeftPlayers();
    }

    private void removeLeftPlayers () {
        // remove from list if player is not on the server anymore
        list.entrySet().removeIf(entry -> bot.players.getEntry(entry.getKey().profile.getId()) == null);
    }

    @Override
    public void playerDisplayNameUpdated (final PlayerEntry target, final Component displayName) {
        final Pair<PlayerEntry, String> player = getFilteredFromName(target.profile.getName());

        if (player == null) return;

        // we use the stringified instead of the component because you can configure the OP and DeOP tag in
        // the extras config
        final String stringifiedDisplayName = ComponentUtilities.stringify(displayName);

        if (stringifiedDisplayName.startsWith("[OP] ")) deOp(target);
    }

    @Override
    public void commandReceived (final PlayerEntry sender, final String command) {
        final Pair<PlayerEntry, String> player = getFilteredFromName(sender.profile.getName());

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
        ) mute(sender, player.getRight());

        deOp(sender);
        gameMode(sender);
        bot.exploits.kick(sender.profile.getId());
    }

    @Override
    public boolean playerMessageReceived (final PlayerMessage message, final ChatPacketType packetType) {
        if (message.sender().profile.getName() == null) return true;

        final Pair<PlayerEntry, String> player = getFilteredFromName(message.sender().profile.getName());

        if (player == null || message.sender().profile.getId().equals(new UUID(0L, 0L))) return true;

        doAll(message.sender(), player.getRight());

        return true;
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        list.clear();
    }

    public void doAll (final PlayerEntry entry) { doAll(entry, ""); }

    public void doAll (final PlayerEntry entry, final String reason) {
        bot.exploits.kick(entry.profile.getId());
        mute(entry, reason);
        deOp(entry);
        gameMode(entry);
        clear(entry);
    }

    public void mute (final PlayerEntry target) { mute(target, ""); }

    public void mute (final PlayerEntry target, final String reason) {
        bot.core.run("essentials:mute " + target.profile.getIdAsString() + " 10y " + reason);
    }

    public void deOp (final PlayerEntry target) {
        bot.core.run("minecraft:execute run deop " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void gameMode (final PlayerEntry target) {
        bot.core.run("minecraft:gamemode spectator " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void clear (final PlayerEntry target) {
        bot.core.run("minecraft:clear " + UUIDUtilities.selector(target.profile.getId()));
    }

    public void kick () {
        for (final PlayerEntry filtered : list.keySet()) {
            bot.exploits.kick(filtered.profile.getId());
        }
    }

    public void add (final PlayerEntry entry, final String reason) {
        if (
                getFilteredFromName(entry.profile.getName()) != null || // prevent already existing filters
                        entry.profile.equals(bot.profile) // prevent self-harm !!!!!!
        ) return;

        list.put(entry, reason);

        doAll(entry, reason);
    }

    public void remove (final String name) {
        list.entrySet().removeIf(filtered -> filtered.getKey().profile.getName().equals(name));
    }

    public Pair<PlayerEntry, String> getFilteredFromName (final String name) {
        for (final Map.Entry<PlayerEntry, String> entry : list.entrySet()) {
            if (entry.getKey().profile.getName().equals(name)) return Pair.of(entry.getKey(), entry.getValue());
        }

        return null;
    }
}
