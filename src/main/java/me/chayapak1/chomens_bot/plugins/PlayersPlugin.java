package me.chayapak1.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import lombok.Getter;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayersPlugin extends SessionAdapter {
    private final Bot bot;
    @Getter private List<MutablePlayerListEntry> list = new ArrayList<>();

    private final List<PlayerListener> listeners = new ArrayList<>();

    public PlayersPlugin (Bot bot) {
        this.bot = bot;
        bot.addListener(this);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerInfoPacket) packetReceived((ClientboundPlayerInfoPacket) packet);
    }

    public void packetReceived (ClientboundPlayerInfoPacket packet) {
        PlayerListEntryAction action = packet.getAction();
        for (PlayerListEntry entry : packet.getEntries()) {
            if (action == PlayerListEntryAction.ADD_PLAYER) addPlayer(entry);
            else if (action == PlayerListEntryAction.UPDATE_GAMEMODE) updateGamemode(entry);
            else if (action == PlayerListEntryAction.UPDATE_LATENCY) updateLatency(entry);
            else if (action == PlayerListEntryAction.UPDATE_DISPLAY_NAME) updateDisplayName(entry);
            else if (action == PlayerListEntryAction.REMOVE_PLAYER) removePlayer(entry);
        }
    }

    public final MutablePlayerListEntry getEntry (UUID uuid) {
        for (MutablePlayerListEntry candidate : list) {
            if (candidate.profile().getId().equals(uuid)) {
                return candidate;
            }
        }

        return null;
    }

    public final MutablePlayerListEntry getEntry (String username) {
        for (MutablePlayerListEntry candidate : list) {
            if (candidate.profile().getName().equals(username)) {
                return candidate;
            }
        }

        return null;
    }

    public final MutablePlayerListEntry getEntry (Component displayName) {
        for (MutablePlayerListEntry candidate : list) {
            if (candidate.displayName() != null && candidate.displayName().equals(displayName)) {
                return candidate;
            }
        }

        return null;
    }

    private MutablePlayerListEntry getEntry (PlayerListEntry other) {
        return getEntry(other.getProfile().getId());
    }

    private void addPlayer (PlayerListEntry newEntry) {
        final MutablePlayerListEntry duplicate = getEntry(newEntry);
        if (duplicate != null) list.remove(duplicate);

        final MutablePlayerListEntry target = new MutablePlayerListEntry(newEntry);

        list.add(target);

        for (PlayerListener listener : listeners) { listener.playerJoined(target); }
    }

    private void updateGamemode (PlayerListEntry newEntry) {
        final MutablePlayerListEntry target = getEntry(newEntry);
        if (target == null) return;

        final GameMode gameMode = newEntry.getGameMode();

        target.gamemode(gameMode);

        for (PlayerListener listener : listeners) { listener.playerGameModeUpdated(target, gameMode); }
    }

    private void updateLatency (PlayerListEntry newEntry) {
        final MutablePlayerListEntry target = getEntry(newEntry);
        if (target == null) return;

        final int ping = newEntry.getPing();

        target.latency(ping);

        for (PlayerListener listener : listeners) { listener.playerLatencyUpdated(target, ping); }
    }

    private void updateDisplayName (PlayerListEntry newEntry) {
        final MutablePlayerListEntry target = getEntry(newEntry);
        if (target == null) return;

        final Component displayName = newEntry.getDisplayName();

        target.displayName(displayName);

        for (PlayerListener listener : listeners) { listener.playerDisplayNameUpdated(target, displayName); }
    }

    private void removePlayer (PlayerListEntry newEntry) {
        final MutablePlayerListEntry target = getEntry(newEntry);
        if (target == null) return;

        bot.tabComplete().tabComplete("/minecraft:scoreboard players add ").thenApply(packet -> {
            final String[] matches = packet.getMatches();
            final Component[] tooltips = packet.getTooltips();
            final String username = target.profile().getName();

            for (int i = 0; i < matches.length; i++) {
                if (tooltips[i] != null || !matches[i].equals(username)) continue;
                return packet;
            }

            list.remove(target);

            for (PlayerListener listener : listeners) { listener.playerLeft(target); }

            return packet;
        });
    }

    public void addListener (PlayerListener listener) { listeners.add(listener); }

    public static class PlayerListener {
        public void playerJoined (MutablePlayerListEntry target) {}
        public void playerGameModeUpdated (MutablePlayerListEntry target, GameMode gameMode) {}
        public void playerLatencyUpdated (MutablePlayerListEntry target, int ping) {}
        public void playerDisplayNameUpdated (MutablePlayerListEntry target, Component displayName) {}
        public void playerLeft (MutablePlayerListEntry target) {}
    }
}
