package me.chayapak1.chomens_bot.plugins;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
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

public class PlayersPlugin implements Listener {
    private final Bot bot;

    public final List<PlayerEntry> list = Collections.synchronizedList(new ObjectArrayList<>());

    private final List<PlayerEntry> pendingLeftPlayers = Collections.synchronizedList(new ObjectArrayList<>());

    public PlayersPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);

        bot.executor.scheduleAtFixedRate(this::onLastKnownNameTick, 0, 5, TimeUnit.SECONDS);
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
                switch (action) {
                    case ADD_PLAYER -> addPlayer(entry);
                    case INITIALIZE_CHAT -> initializeChat(entry);
                    case UPDATE_LISTED -> updateListed(entry);
                    case UPDATE_GAME_MODE -> updateGameMode(entry);
                    case UPDATE_LATENCY -> updateLatency(entry);
                    case UPDATE_DISPLAY_NAME -> updateDisplayName(entry);
                    default -> { }
                }
            }
        }
    }

    private void packetReceived (final ClientboundPlayerInfoRemovePacket packet) {
        final List<UUID> uuids = packet.getProfileIds();

        for (final UUID uuid : uuids) removePlayer(uuid);
    }

    private void queryPlayersIP () {
        synchronized (list) {
            for (final PlayerEntry target : list) queryPlayersIP(target);
        }
    }

    private void queryPlayersIP (final PlayerEntry target) {
        if (target.persistingData.ip != null) return;

        final CompletableFuture<String> future = getPlayerIP(target, false);

        future.thenApply(ip -> {
            if (ip == null) return null;

            target.persistingData.ip = ip;

            bot.listener.dispatch(listener -> listener.onQueriedPlayerIP(target, ip));

            return null;
        });
    }

    public CompletableFuture<String> getPlayerIP (final PlayerEntry target, final boolean fallbackToDatabase) {
        final CompletableFuture<String> seenFuture = getSeenPlayerIP(target);

        if (seenFuture != null) return seenFuture;
        else if (fallbackToDatabase) return getDatabasePlayerIP(target);
        else return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<String> getDatabasePlayerIP (final PlayerEntry target) {
        final CompletableFuture<String> outputFuture = new CompletableFuture<>();

        DatabasePlugin.EXECUTOR_SERVICE.execute(
                () -> outputFuture.complete(bot.playersDatabase.getPlayerIP(target.profile.getName()))
        );

        return outputFuture;
    }

    private CompletableFuture<String> getSeenPlayerIP (final PlayerEntry target) {
        final CompletableFuture<String> outputFuture = new CompletableFuture<>();

        if (
                !bot.serverFeatures.hasEssentials ||
                        (
                                !bot.serverFeatures.serverHasCommand("essentials:seen")
                                        && !bot.serverFeatures.serverHasCommand("seen")
                        )
        ) {
            return null;
        }

        final CompletableFuture<Component> trackedCoreFuture = bot.core.runTracked("essentials:seen " + target.profile.getIdAsString());

        if (trackedCoreFuture == null) return null;

        trackedCoreFuture.completeOnTimeout(null, 5, TimeUnit.SECONDS);

        trackedCoreFuture.thenApply(output -> {
            final List<Component> children = output.children();

            String string = ComponentUtilities
                    .stringify(Component.join(JoinConfiguration.separator(Component.empty()), children))
                    .trim();

            if (!string.startsWith("- IP Address: ")) return null;

            string = string.substring("- IP Address: ".length());
            if (string.startsWith("/")) string = string.substring(1);

            outputFuture.complete(string);

            return null;
        });

        return outputFuture;
    }

    public final PlayerEntry getEntry (final UUID uuid) {
        synchronized (list) {
            for (final PlayerEntry candidate : list) {
                if (candidate != null && candidate.profile.getId().equals(uuid)) {
                    return candidate;
                }
            }

            return null;
        }
    }

    public final PlayerEntry getEntry (final String username) { return getEntry(username, true, true); }

    public final PlayerEntry getEntry (final String username, final boolean checkUUID, final boolean checkPastUsernames) {
        synchronized (list) {
            for (final PlayerEntry candidate : list) {
                if (
                        candidate != null &&
                                (
                                        candidate.profile.getName().equals(username)
                                                || (checkUUID && candidate.profile.getIdAsString().equals(username))
                                                || (checkPastUsernames && candidate.persistingData.usernames.contains(username))
                                )
                ) {
                    return candidate;
                }
            }

            return null;
        }
    }

    // RIPPED from craftbukkit (literally)
    // https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/browse/src/main/java/org/bukkit/craftbukkit/CraftServer.java#592
    public final PlayerEntry getEntryTheBukkitWay (final String username) {
        PlayerEntry found = getEntry(username, false, false);
        // Try for an exact match first.
        if (found != null) {
            return found;
        }

        final String lowerName = username.toLowerCase(Locale.ROOT);
        int delta = Integer.MAX_VALUE;
        synchronized (list) {
            for (final PlayerEntry player : list) {
                if (player.profile.getName().toLowerCase(Locale.ROOT).startsWith(lowerName)) {
                    final int curDelta = Math.abs(player.profile.getName().length() - lowerName.length());
                    if (curDelta < delta) {
                        found = player;
                        delta = curDelta;
                    }
                    if (curDelta == 0) break;
                }
            }
        }
        return found;
    }

    public final PlayerEntry getEntry (final Component displayName) {
        synchronized (list) {
            for (final PlayerEntry candidate : list) {
                if (candidate != null && candidate.displayName != null && candidate.displayName.equals(displayName)) {
                    return candidate;
                }
            }

            return null;
        }
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

        target.persistingData.listed = newEntry.isListed();
    }

    private void addPlayer (final PlayerListEntry newEntry) {
        final PlayerEntry duplicate = getEntry(newEntry);

        final PlayerEntry target = new PlayerEntry(newEntry);

        if (duplicate != null && !duplicate.profile.getName().equals(target.profile.getName())) return;

        if (duplicate != null) {
            list.removeIf(entry -> entry.equals(duplicate));

            target.persistingData = new PlayerEntry.PersistingData(duplicate.persistingData);
            target.persistingData.listed = true;

            list.add(target);

            bot.listener.dispatch(listener -> listener.onPlayerUnVanished(target));
        } else {
            list.add(target);

            bot.listener.dispatch(listener -> listener.onPlayerJoined(target));

            queryPlayersIP(target);
        }
    }

    private void updateGameMode (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final GameMode gameMode = newEntry.getGameMode();

        target.gamemode = gameMode;

        bot.listener.dispatch(listener -> listener.onPlayerGameModeUpdated(target, gameMode));
    }

    private void updateLatency (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final int ping = newEntry.getLatency();

        target.latency = ping;

        bot.listener.dispatch(listener -> listener.onPlayerLatencyUpdated(target, ping));
    }

    private void updateDisplayName (final PlayerListEntry newEntry) {
        final PlayerEntry target = getEntry(newEntry);
        if (target == null) return;

        final Component displayName = newEntry.getDisplayName();

        target.displayName = displayName;

        bot.listener.dispatch(listener -> listener.onPlayerDisplayNameUpdated(target, displayName));
    }

    private CompletableFuture<String> getLastKnownName (final boolean useCargo, final String uuid) {
        return bot.query.entity(useCargo, uuid, "bukkit.lastKnownName");
    }

    private void check (final boolean useCargo, final PlayerEntry target) {
        final PlayerEntry pending = pendingLeftPlayers.stream()
                .filter(player -> player.equals(target))
                .findAny()
                .orElse(null);

        if (pending != null) pendingLeftPlayers.remove(pending);

        final CompletableFuture<String> future = getLastKnownName(useCargo, target.profile.getIdAsString());

        future.thenApply(lastKnownName -> {
            // checking for empty string means
            // we did get the output from the server, and
            // the player doesn't exist or the NBT path doesn't exist
            // i also check for empty target name for the empty string username kind of people
            if (lastKnownName == null && !target.profile.getName().isEmpty()) {
                final boolean removed = list.remove(target);

                // checking if removed prevents the event from being called twice
                // this was a bug for quite a few weeks lol
                if (removed) {
                    bot.listener.dispatch(listener -> listener.onPlayerLeft(target));
                }

                return null;
            } else if (lastKnownName != null && !lastKnownName.equals(target.profile.getName())) {
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
                        target.persistingData.listed
                );

                newTarget.persistingData = new PlayerEntry.PersistingData(target);

                list.removeIf(entry -> entry.profile.getId().equals(target.profile.getId()));

                list.add(newTarget);

                bot.listener.dispatch(listener -> listener.onPlayerChangedUsername(
                        newTarget, target.profile.getName(), newTarget.profile.getName()
                ));
            } else if (pending != null) {
                // we already passed all the left and username check,
                // so the only one left is vanish

                target.persistingData.listed = false;

                bot.listener.dispatch(listener -> listener.onPlayerVanished(target));
            }

            return null;
        });
    }

    private void onLastKnownNameTick () {
        // hasNamespaces also means vanilla/non-bukkit
        if (!bot.loggedIn || !bot.core.ready || !bot.serverFeatures.hasNamespaces)
            return;

        for (final PlayerEntry target : new ArrayList<>(list)) {
            check(true, target);
        }
    }

    private void removePlayer (final UUID uuid) {
        final PlayerEntry target = getEntry(uuid);

        if (target == null) return;

        if (
                (System.currentTimeMillis() - bot.loginTime > 5 * 1000 && !bot.serverFeatures.hasNamespaces)
                        || target.profile.getName().isEmpty() // LoL empty string username lazy fix
        ) {
            final boolean removed = list.remove(target);

            if (removed) {
                bot.listener.dispatch(listener -> listener.onPlayerLeft(target));
            }
        } else {
            pendingLeftPlayers.add(target);
            check(false, target);
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        list.clear();
    }
}
