package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoRemovePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayersPlugin extends Bot.Listener implements TickPlugin.Listener {
    private final Bot bot;

    public final List<PlayerEntry> list = new ArrayList<>();

    private final List<Listener> listeners = new ArrayList<>();

    public PlayersPlugin (Bot bot) {
        this.bot = bot;

        bot.executor.scheduleAtFixedRate(this::onLastKnownNameTick, 0, 5, TimeUnit.SECONDS);

        bot.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onSecondTick () {
        queryPlayersIP();
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundPlayerInfoRemovePacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (ClientboundPlayerInfoUpdatePacket packet) {
        final EnumSet<PlayerListEntryAction> actions = packet.getActions();

        for (PlayerListEntryAction action : actions) {
            for (PlayerListEntry entry : packet.getEntries()) {
                if (action == PlayerListEntryAction.ADD_PLAYER) addPlayer(entry);
                else if (action == PlayerListEntryAction.INITIALIZE_CHAT) initializeChat(entry);
                else if (action == PlayerListEntryAction.UPDATE_LISTED) updateListed(entry);
                else if (action == PlayerListEntryAction.UPDATE_GAME_MODE) updateGameMode(entry);
                else if (action == PlayerListEntryAction.UPDATE_LATENCY) updateLatency(entry);
                else if (action == PlayerListEntryAction.UPDATE_DISPLAY_NAME) updateDisplayName(entry);
            }
        }
    }

    private void packetReceived (ClientboundPlayerInfoRemovePacket packet) {
        final List<UUID> uuids = packet.getProfileIds();

        for (UUID uuid : uuids) removePlayer(uuid);
    }

    private void queryPlayersIP () { for (PlayerEntry target : list) queryPlayersIP(target); }

    private void queryPlayersIP (PlayerEntry target) {
        if (target.ip != null) return;

        final CompletableFuture<String> future = getPlayerIP(target, true);

        future.thenApply(ip -> target.ip = ip);
    }

    public CompletableFuture<String> getPlayerIP (PlayerEntry target) { return getPlayerIP(target, false); }

    public CompletableFuture<String> getPlayerIP (PlayerEntry target, boolean forceSeen) {
        final CompletableFuture<String> outputFuture = new CompletableFuture<>();

        DatabasePlugin.executorService.submit(() -> {
            if (!forceSeen) {
                final String databaseIP = bot.playersDatabase.getPlayerIP(target.profile.getName());

                if (databaseIP != null) {
                    outputFuture.complete(databaseIP);
                    return;
                }
            }

            if (!bot.serverFeatures.hasEssentials) {
                outputFuture.complete(null);
                return;
            }

            final CompletableFuture<Component> trackedCoreFuture = bot.core.runTracked("essentials:seen " + target.profile.getIdAsString());

            if (trackedCoreFuture == null) {
                outputFuture.complete(null);
                return;
            }

            trackedCoreFuture.completeOnTimeout(null, 5, TimeUnit.SECONDS);

            trackedCoreFuture.thenApply(output -> {
                final List<Component> children = output.children();

                String stringified = ComponentUtilities.stringify(Component.join(JoinConfiguration.separator(Component.empty()), children));

                if (!stringified.startsWith(" - IP Address: ")) return output;

                stringified = stringified.trim().substring(" - IP Address: ".length());
                if (stringified.startsWith("/")) stringified = stringified.substring(1);

                outputFuture.complete(stringified);

                return output;
            });
        });

        return outputFuture;
    }

    public final PlayerEntry getEntry (UUID uuid) {
        for (PlayerEntry candidate : list) {
            if (candidate != null && candidate.profile.getId().equals(uuid)) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (String username) {
        for (PlayerEntry candidate : list) {
            if (
                    candidate != null &&
                            (
                                    candidate.profile.getIdAsString().equals(username) || // checks for a string UUID
                                            getName(candidate).equals(username) ||
                                            candidate.usernames.contains(username)
                            )
            ) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (Component displayName) {
        for (PlayerEntry candidate : list) {
            if (candidate != null && candidate.displayName != null && candidate.displayName.equals(displayName)) {
                return candidate;
            }
        }

        return null;
    }

    public PlayerEntry getBotEntry () { return getEntry(bot.username); }

    private PlayerEntry getEntry (PlayerListEntry other) {
        return getEntry(other.getProfileId());
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

    private String getName (PlayerEntry target) {
        return bot.options.creayun ?
                target.profile.getName().replaceAll("§.", "") :
                target.profile.getName();
    }

    private void addPlayer (PlayerListEntry newEntry) {
        final PlayerEntry duplicate = getEntry(newEntry);

        final PlayerEntry target = new PlayerEntry(newEntry);

        if (duplicate != null && !duplicate.profile.getName().equals(target.profile.getName())) return;

        if (duplicate != null) {
            list.remove(duplicate);

            target.listed = true;

            target.usernames.addAll(duplicate.usernames);
            target.ip = duplicate.ip;

            list.add(target);

            for (Listener listener : listeners) listener.playerUnVanished(target);
        } else {
            list.add(target);

            queryPlayersIP(target);

            for (Listener listener : listeners) listener.playerJoined(target);
        }
    }

    private void updateGameMode (PlayerListEntry newEntry) {
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

    private CompletableFuture<String> getLastKnownName (String uuid) {
        return bot.query.entity(uuid, "bukkit.lastKnownName");
    }

    private void check (PlayerEntry target) {
        final CompletableFuture<String> future = getLastKnownName(target.profile.getIdAsString());

        future.thenApply(lastKnownName -> {
            // checking for empty string means
            // we did get the output from the server, and
            // the player doesn't exist or the NBT path doesn't exist
            // i also check for empty target name for the empty string username kind of people
            if (lastKnownName.isEmpty() && !target.profile.getName().isEmpty()) {
                final boolean removed = list.remove(target);

                // checking if removed prevents the event from being called twice
                // this was a bug for quite a few weeks lol
                if (removed) {
                    for (Listener listener : listeners) listener.playerLeft(target);
                }
            } else if (!lastKnownName.equals(target.profile.getName())) {
                final PlayerEntry newTarget = new PlayerEntry(
                        new GameProfile(
                                target.profile.getId(),
                                lastKnownName
                        ),
                        target.gamemode,
                        target.latency,
                        target.displayName,
                        target.expiresAt,
                        target.publicKey,
                        target.keySignature,
                        target.listed
                );

                newTarget.usernames.addAll(target.usernames);
                newTarget.usernames.addLast(target.profile.getName());

                newTarget.ip = target.ip;

                list.add(newTarget);

                list.remove(target);

                for (Listener listener : listeners) listener.playerChangedUsername(newTarget);
            }

            return null;
        });
    }

    private void onLastKnownNameTick () {
        if (!bot.loggedIn || !bot.core.ready || !bot.serverFeatures.hasNamespaces)
            return; // hasNamespaces also means vanilla/non-bukkit

        for (PlayerEntry target : new ArrayList<>(list)) {
            check(target);
        }
    }

    private void removePlayer (UUID uuid) {
        final PlayerEntry target = getEntry(uuid);

        if (target == null) return;

        if (!bot.serverFeatures.hasNamespaces || target.profile.getName().isEmpty() /* LoL empty string username lazy fix */) {
            final boolean removed = list.remove(target);

            if (removed) {
                for (Listener listener : listeners) listener.playerLeft(target);
            }
        } else {
            check(target);
        }
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        list.clear();
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    @SuppressWarnings("unused")
    public interface Listener {
        default void playerJoined (PlayerEntry target) { }

        default void playerUnVanished (PlayerEntry target) { }

        default void playerGameModeUpdated (PlayerEntry target, GameMode gameMode) { }

        default void playerLatencyUpdated (PlayerEntry target, int ping) { }

        default void playerDisplayNameUpdated (PlayerEntry target, Component displayName) { }

        default void playerLeft (PlayerEntry target) { }

        default void playerVanished (PlayerEntry target) { }

        default void playerChangedUsername (PlayerEntry target) { }
    }
}
