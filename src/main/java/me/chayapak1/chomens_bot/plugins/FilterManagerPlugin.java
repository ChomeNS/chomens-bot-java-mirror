package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.filter.FilteredPlayer;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class FilterManagerPlugin implements Listener {
    private final Bot bot;

    public final List<FilteredPlayer> list = Collections.synchronizedList(new ObjectArrayList<>());

    public FilterManagerPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);

        bot.executor.scheduleAtFixedRate(this::kick, 0, 10, TimeUnit.SECONDS);
    }

    @Override
    public void onLocalSecondTick () {
        removeLeftPlayers();
    }

    private void removeLeftPlayers () {
        synchronized (list) {
            // remove from list if player is not on the server anymore
            list.removeIf(filteredPlayer -> bot.players.getEntry(filteredPlayer.player().profile.getId()) == null);
        }
    }

    @Override
    public void onTick () {
        synchronized (list) {
            for (final FilteredPlayer filtered : list) {
                final PlayerEntry target = filtered.player();

                deOp(target);
                gameMode(target);
                clear(target);
            }
        }
    }

    private void kick () {
        synchronized (list) {
            for (final FilteredPlayer filtered : list) {
                final PlayerEntry target = filtered.player();
                bot.exploits.kick(target.profile.getId());
            }
        }
    }

    @Override
    public void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) {
        final FilteredPlayer filtered = getFilteredFromName(sender.profile.getName());

        if (filtered == null) return;

        if (
                command.startsWith("/mute") ||
                        command.startsWith("/emute") ||
                        command.startsWith("/silence") ||
                        command.startsWith("/esilence") ||
                        command.startsWith("/essentials:mute") ||
                        command.startsWith("/essentials:emute") ||
                        command.startsWith("/essentials:silence") ||
                        command.startsWith("/essentials:esilence")
        ) mute(sender, filtered.reason());

        deOp(sender);
        gameMode(sender);
        bot.exploits.kick(sender.profile.getId());
    }

    @Override
    public boolean onPlayerMessageReceived (final PlayerMessage message, final ChatPacketType packetType) {
        if (message.sender().profile.getName() == null) return true;

        final FilteredPlayer filtered = getFilteredFromName(message.sender().profile.getName());

        if (filtered == null || message.sender().profile.getId().equals(new UUID(0L, 0L))) return true;

        doAll(message.sender(), filtered.reason());

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

    public void add (final PlayerEntry entry, final String reason) {
        if (
                getFilteredFromName(entry.profile.getName()) != null || // prevent already existing filters
                        entry.profile.equals(bot.profile) // prevent self-harm !!!!!!
        ) return;

        list.add(new FilteredPlayer(entry, reason));

        doAll(entry, reason);
    }

    public void remove (final String name) {
        synchronized (list) {
            list.removeIf(filtered -> filtered.player().profile.getName().equals(name));
        }
    }

    public @Nullable FilteredPlayer getFilteredFromName (final String name) {
        synchronized (list) {
            for (final FilteredPlayer filtered : list) {
                if (filtered.player().profile.getName().equals(name)) return filtered;
            }
        }

        return null;
    }
}
