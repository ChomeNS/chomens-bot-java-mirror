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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PlayersPlugin extends Bot.Listener implements TickPlugin.Listener {
    private final Bot bot;

    public final List<PlayerEntry> list = Collections.synchronizedList(new ArrayList<>());

    private final List<Listener> listeners = new ArrayList<>();

    private final List<PlayerEntry> pendingLeftPlayers = Collections.synchronizedList(new ArrayList<>());

    public PlayersPlugin (final Bot bot) {
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
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundPlayerInfoUpdatePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundPlayerInfoRemovePacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundPlayerInfoUpdatePacket packet) {
        final EnumSet<PlayerListEntryAction> actions = packet.getActions();

        for (final PlayerListEntryAction action : actions) {
            for (final PlayerListEntry entry : packet.getEntries()) {
                if (action == PlayerListEntryAction.ADD_PLAYER) addPlayer(entry);
                else if (action == PlayerListEntryAction.INITIALIZE_CHAT) initializeChat(entry);
                else if (action == PlayerListEntryAction.UPDATE_LISTED) updateListed(entry);
                else if (action == PlayerListEntryAction.UPDATE_GAME_MODE) updateGameMode(entry);
                else if (action == PlayerListEntryAction.UPDATE_LATENCY) updateLatency(entry);
                else if (action == PlayerListEntryAction.UPDATE_DISPLAY_NAME) updateDisplayName(entry);
            }
        }
    }

    private void packetReceived (final ClientboundPlayerInfoRemovePacket packet) {
        final List<UUID> uuids = packet.getProfileIds();

        for (final UUID uuid : uuids) removePlayer(uuid);
    }

    private void queryPlayersIP () { for (final PlayerEntry target : list) queryPlayersIP(target); }

    private void queryPlayersIP (final PlayerEntry target) {
        if (target.ip != null) return;

        final CompletableFuture<String> future = getPlayerIP(target, true);

        future.thenApply(ip -> {
            target.ip = ip;

            for (final Listener listener : listeners) listener.queriedPlayerIP(target, ip);

            return null;
        });
    }

    public CompletableFuture<String> getPlayerIP (final PlayerEntry target) { return getPlayerIP(target, false); }

    public CompletableFuture<String> getPlayerIP (final PlayerEntry target, final boolean forceSeen) {
        final CompletableFuture<String> outputFuture = new CompletableFuture<>();

        DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
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

            final CompletableFuture<Component> trackedCoreFuture = bot.core.runTracked(true, "essentials:seen " + target.profile.getIdAsString());

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

    public final PlayerEntry getEntry (final UUID uuid) {
        for (final PlayerEntry candidate : list) {
            if (candidate != null && candidate.profile.getId().equals(uuid)) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (final String username) {
        for (final PlayerEntry candidate : list) {
            if (
                    candidate != null &&
                            (
                                    candidate.profile.getIdAsString().equals(username) || // checks for a string UUID
                                            candidate.profile.getName().equals(username) ||
                                            candidate.usernames.contains(username)
                            )
            ) {
                return candidate;
            }
        }

        return null;
    }

    public final PlayerEntry getEntry (final Component displayName) {
        for (final PlayerEntry candidate : list) {
            if (candidate != null && candidate.displayName != null && candidate.displayName.equals(displayName)) {
                return candidate;
            }
        }

        return null;
    }

    public PlayerEntry getBotEntry () { return getEntry(bot.username); }

    private PlayerEntry getEntry (final PlayerListEntry other) {
        return getEntry(other.getProfileId());
    }

    private void initializeChat (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        target.publicKey = newEntry.getPublicKey();
    }

    private void updateListed (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        target.listed = newEntry.isListed();
    }

    private void addPlayer (final PlayerListEntry newEntry) {
        final PlayerEntry duplicate = getEntry(newEntry);

        final PlayerEntry target = new PlayerEntry(newEntry);

        if (duplicate != null && !duplicate.profile.getName().equals(target.profile.getName())) return;

        if (duplicate != null) {
            list.removeIf(entry -> entry.equals(duplicate));

            target.listed = true;

            target.usernames.addAll(duplicate.usernames);
            target.ip = duplicate.ip;

            list.add(target);

            for (final Listener listener : listeners) listener.playerUnVanished(target);
        } else {
            list.add(target);

            queryPlayersIP(target);

            for (final Listener listener : listeners) listener.playerJoined(target);
        }
    }

    private void updateGameMode (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final GameMode gameMode = newEntry.getGameMode();

        target.gamemode = gameMode;

        for (final Listener listener : listeners) { listener.playerGameModeUpdated(target, gameMode); }
    }

    private void updateLatency (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final int ping = newEntry.getLatency();

        target.latency = ping;

        for (final Listener listener : listeners) { listener.playerLatencyUpdated(target, ping); }
    }

    private void updateDisplayName (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final Component displayName = newEntry.getDisplayName();

        target.displayName = displayName;

        for (final Listener listener : listeners) { listener.playerDisplayNameUpdated(target, displayName); }
    }

    private CompletableFuture<String> getLastKnownName (final String uuid) {
        return bot.query.entity(true, uuid, "bukkit.lastKnownName");
    }

    private void check (final PlayerEntry target) {
        final PlayerEntry pending = pendingLeftPlayers.stream()
                .filter(player -> player.equals(target))
                .findAny()
                .orElse(null);

        if (pending != null) pendingLeftPlayers.remove(pending);

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
                    for (final Listener listener : listeners) listener.playerLeft(target);
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

                list.removeIf(entry -> entry.profile.getId().equals(target.profile.getId()));

                list.add(newTarget);

                for (final Listener listener : listeners) listener.playerChangedUsername(newTarget);
            } else if (pending != null) {
                // we already passed all the left and username check,
                // so the only one left is vanish

                target.listed = false;

                for (final Listener listener : listeners) listener.playerVanished(target);
            }

            return null;
        });
    }

    private void onLastKnownNameTick () {
        // hasNamespaces also means vanilla/non-bukkit
        if (!bot.loggedIn || !bot.core.ready || !bot.serverFeatures.hasNamespaces)
            return;

        for (final PlayerEntry target : new ArrayList<>(list)) {
            check(target);
        }
    }

    private void removePlayer (final UUID uuid) {
        final PlayerEntry target = getEntry(uuid);

        if (target == null) return;

        if (!bot.serverFeatures.hasNamespaces || target.profile.getName().isEmpty() /* LoL empty string username lazy fix */) {
            final boolean removed = list.remove(target);

            if (removed) {
                for (final Listener listener : listeners) listener.playerLeft(target);
            }
        } else {
            pendingLeftPlayers.add(target);

            check(target);
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        list.clear();
    }

    public void addListener (final Listener listener) { listeners.add(listener); }

    @SuppressWarnings("unused")
    public interface Listener {
        default void playerJoined (final PlayerEntry target) { }

        default void playerUnVanished (final PlayerEntry target) { }

        default void playerGameModeUpdated (final PlayerEntry target, final GameMode gameMode) { }

        default void playerLatencyUpdated (final PlayerEntry target, final int ping) { }

        default void playerDisplayNameUpdated (final PlayerEntry target, final Component displayName) { }

        default void playerLeft (final PlayerEntry target) { }

        default void playerVanished (final PlayerEntry target) { }

        default void playerChangedUsername (final PlayerEntry target) { }

        default void queriedPlayerIP (final PlayerEntry target, final String ip) { }
    }
}
