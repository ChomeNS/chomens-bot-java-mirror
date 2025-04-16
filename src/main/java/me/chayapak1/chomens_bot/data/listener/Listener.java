package me.chayapak1.chomens_bot.data.listener;

import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.chat.PlayerMessage;
import me.chayapak1.chomens_bot.data.entity.Rotation;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;

import java.util.UUID;

@SuppressWarnings("unused")
public interface Listener {
    // from mcprotocollib SessionListener
    default void packetReceived (final Session session, final Packet packet) { }
    default void packetSending (final PacketSendingEvent event) { }
    default void packetSent (final Session session, final Packet packet) { }
    default void packetError (final PacketErrorEvent event) { }
    default void connected (final ConnectedEvent event) { }
    default void disconnecting (final DisconnectingEvent event) { }
    default void disconnected (final DisconnectedEvent event) { }

    // bot
    default void onConnecting () { }

    // ticker
    default void onTick () { }
    default void onAlwaysTick () { }
    default void onSecondTick () { }

    // core
    default void onCoreReady () { }
    default void onCoreRefilled () { }

    // position
    default void onPositionChange (final Vector3d position) { }
    default void onPlayerMoved (final PlayerEntry player, final Vector3d position, final Rotation rotation) { }

    // world
    default void onWorldChanged (final String dimension) { }

    // players
    default void onPlayerJoined (final PlayerEntry target) { }
    default void onPlayerUnVanished (final PlayerEntry target) { }
    default void onPlayerGameModeUpdated (final PlayerEntry target, final GameMode gameMode) { }
    default void onPlayerLatencyUpdated (final PlayerEntry target, final int ping) { }
    default void onPlayerDisplayNameUpdated (final PlayerEntry target, final Component displayName) { }
    default void onPlayerLeft (final PlayerEntry target) { }
    default void onPlayerVanished (final PlayerEntry target) { }
    default void onPlayerChangedUsername (final PlayerEntry target, final String from, final String to) { }
    default void onQueriedPlayerIP (final PlayerEntry target, final String ip) { }

    // ChomeNS Mod
    default void onChomeNSModPacketReceived (final PlayerEntry player, final me.chayapak1.chomens_bot.chomeNSMod.Packet packet) { }

    // chat
    default boolean onPlayerMessageReceived (final PlayerMessage message, final ChatPacketType packetType) { return true; }
    default boolean onSystemMessageReceived (final Component component, final String string, final String ansi) { return true; }

    // commandspy
    default void onCommandSpyMessageReceived (final PlayerEntry sender, final String command) { }

    // extras messenger
    default void onExtrasMessageReceived (final UUID sender, final byte[] message) { }
}
