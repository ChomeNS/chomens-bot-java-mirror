package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.PlayerListEntry;
import com.github.steveice10.mc.protocol.data.game.PlayerListEntryAction;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.PersistentDataUtilities;
import net.kyori.adventure.text.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class PlayersPlugin extends Bot.Listener {
    private final Bot bot;

    public final List<PlayerEntry> list = new ArrayList<>();

    private final List<Listener> listeners = new ArrayList<>();

    public static JsonObject playersObject = new JsonObject();

    static {
        if (PersistentDataUtilities.jsonObject.has("players")) {
            playersObject = PersistentDataUtilities.jsonObject.get("players").getAsJsonObject();
        }
    }

    public PlayersPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket) packetReceived((ClientboundPlayerInfoUpdatePacket) packet);
        else if (packet instanceof ClientboundPlayerInfoRemovePacket) packetReceived((ClientboundPlayerInfoRemovePacket) packet);
    }

    public void packetReceived (ClientboundPlayerInfoUpdatePacket packet) {
        EnumSet<PlayerListEntryAction> actions = packet.getActions();
        for (PlayerListEntryAction action : actions) {
            for (PlayerListEntry entry : packet.getEntries()) {
                if (action == PlayerListEntryAction.ADD_PLAYER) addPlayer(entry);
                else if (action == PlayerListEntryAction.INITIALIZE_CHAT) initializeChat(entry);
                else if (action == PlayerListEntryAction.UPDATE_LISTED) updateListed(entry);
                else if (action == PlayerListEntryAction.UPDATE_GAME_MODE) updateGamemode(entry);
                else if (action == PlayerListEntryAction.UPDATE_LATENCY) updateLatency(entry);
                else if (action == PlayerListEntryAction.UPDATE_DISPLAY_NAME) updateDisplayName(entry);
            }
        }
    }

    public void packetReceived (ClientboundPlayerInfoRemovePacket packet) {
        final List<UUID> uuids = packet.getProfileIds();

        for (UUID uuid : uuids) {
            removePlayer(uuid);
        }
    }

    public final PlayerEntry getEntry (UUID uuid) {
        for (PlayerEntry candidate : list) {
            if (candidate.profile.getId().equals(uuid)) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (String username) {
        for (PlayerEntry candidate : list) {
            if (getName(candidate).equals(username)) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (Component displayName) {
        for (PlayerEntry candidate : list) {
            if (candidate.displayName != null && candidate.displayName.equals(displayName)) {
                return candidate;
            }
        }

        return null;
    }

    public PlayerEntry getBotEntry () { return getEntry(bot.username); }

    private PlayerEntry getEntry (PlayerListEntry other) {
        return getEntry(other.getProfile().getId());
    }

    private void initializeChat (PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        target.publicKey = newEntry.getPublicKey();
    }

    private void updateListed (PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        target.listed = newEntry.isListed();
    }

    private String getName(PlayerEntry target) {
        return bot.options.creayun ? target.profile.getName().replaceAll("§.", "") : target.profile.getName();
    }

    private void addPlayer (PlayerListEntry newEntry) {
        final PlayerEntry duplicate = getEntry(newEntry);
        if (duplicate != null) list.remove(duplicate);

        final PlayerEntry target = new PlayerEntry(newEntry);

        list.add(target);

        if (duplicate == null) {
            for (Listener listener : listeners) { listener.playerJoined(target); }

            // should this be here?
            if (playersObject.has(getName(target))) return;

            final JsonObject object = new JsonObject();
            object.addProperty("uuid", target.profile.getIdAsString());
            object.add("lastSeen", new JsonObject());

            playersObject.add(getName(target), object);

            PersistentDataUtilities.put("players", playersObject);
        } else for (Listener listener : listeners) { listener.playerUnVanished(target); }
    }

    private void updateGamemode (PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final GameMode gameMode = newEntry.getGameMode();

        target.gamemode = gameMode;

        for (Listener listener : listeners) { listener.playerGameModeUpdated(target, gameMode); }
    }

    private void updateLatency (PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final int ping = newEntry.getLatency();

        target.latency = ping;

        for (Listener listener : listeners) { listener.playerLatencyUpdated(target, ping); }
    }

    private void updateDisplayName (PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final Component displayName = newEntry.getDisplayName();

        target.displayName = displayName;

        for (Listener listener : listeners) { listener.playerDisplayNameUpdated(target, displayName); }
    }

    private void removePlayer (UUID uuid) {
        final PlayerEntry target = getEntry(uuid);
        if (target == null) return;

        bot.tabComplete.tabComplete("/minecraft:scoreboard players add ").thenApply(packet -> {
            final String[] matches = packet.getMatches();
            final Component[] tooltips = packet.getTooltips();
            final String username = target.profile.getName();

            for (int i = 0; i < matches.length; i++) {
                if (tooltips[i] != null || !matches[i].equals(username)) continue;
                target.listed = false;
                for (Listener listener : listeners) { listener.playerVanished(target); }
                return packet;
            }

            list.remove(target);

            for (Listener listener : listeners) { listener.playerLeft(target); }

            // should this be here?
            if (!playersObject.has(getName(target))) return packet;

            final JsonObject player = playersObject.get(getName(target)).getAsJsonObject();

            final JsonObject object = new JsonObject();
            object.addProperty("time", Instant.now().toEpochMilli());
            object.addProperty("server", bot.host + ":" + bot.port);

            player.add("lastSeen", object);

            PersistentDataUtilities.put("players", playersObject);

            return packet;
        });
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        list.clear();
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    public static class Listener {
        public void playerJoined (PlayerEntry target) {}
        public void playerUnVanished (PlayerEntry target) {}
        public void playerGameModeUpdated (PlayerEntry target, GameMode gameMode) {}
        public void playerLatencyUpdated (PlayerEntry target, int ping) {}
        public void playerDisplayNameUpdated (PlayerEntry target, Component displayName) {}
        public void playerLeft (PlayerEntry target) {}
        public void playerVanished (PlayerEntry target) {}
    }
}
