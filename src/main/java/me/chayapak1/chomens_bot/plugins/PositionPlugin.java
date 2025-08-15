package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.entity.EntityData;
import me.chayapak1.chomens_bot.data.entity.Rotation;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.MathUtilities;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PositionElement;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// the player position and rotation tracking are currently unused, but i'll keep it for future use
@SuppressWarnings("unused")
public class PositionPlugin implements Listener {
    private final Bot bot;

    public Vector3d position = Vector3d.from(0, 0, 0);

    public boolean isGoingDownFromHeightLimit = false; // cool variable name
    private long tpCommandCooldownTime = 0;

    public final Map<Integer, EntityData> entityIdToData = new ConcurrentHashMap<>();

    public PositionPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onTick () {
        handleHeightLimit();
    }

    // Notchian clients also do this, where it sends the current position to the server every second
    @Override
    public void onSecondTick () {
        if (isGoingDownFromHeightLimit) return;

        bot.session.send(new ServerboundMovePlayerPosPacket(
                false,
                false,
                position.getX(),
                position.getY(),
                position.getZ()
        ));
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        switch (packet) {
            case final ClientboundPlayerPositionPacket t_packet -> packetReceived(t_packet);
            case final ClientboundAddEntityPacket t_packet -> packetReceived(t_packet);
            case final ClientboundRemoveEntitiesPacket t_packet -> packetReceived(t_packet);
            case final ClientboundMoveEntityRotPacket t_packet -> packetReceived(t_packet);
            case final ClientboundMoveEntityPosPacket t_packet -> packetReceived(t_packet);
            case final ClientboundMoveEntityPosRotPacket t_packet -> packetReceived(t_packet);
            case final ClientboundEntityPositionSyncPacket t_packet -> packetReceived(t_packet);
            default -> { }
        }
    }

    private void packetReceived (final ClientboundPlayerPositionPacket packet) {
        bot.session.send(new ServerboundAcceptTeleportationPacket(packet.getId()));

        final List<PositionElement> relatives = packet.getRelatives();

        this.position = packet.getPosition().add(
                relatives.contains(PositionElement.X) ? this.position.getX() : 0,
                relatives.contains(PositionElement.Y) ? this.position.getY() : 0,
                relatives.contains(PositionElement.Z) ? this.position.getZ() : 0
        );

        bot.listener.dispatch(listener -> listener.onPositionChange(position));
    }

    private void packetReceived (final ClientboundAddEntityPacket packet) {
        if (packet.getType() != EntityType.PLAYER) return;

        final PlayerEntry entry = bot.players.getEntry(packet.getUuid());

        if (entry == null) return;

        entityIdToData.remove(packet.getEntityId());
        entityIdToData.put(
                packet.getEntityId(),
                new EntityData(
                        entry,
                        Vector3d.from(packet.getX(), packet.getY(), packet.getZ()),
                        new Rotation(packet.getYaw(), packet.getPitch())
                )
        );
    }

    private void packetReceived (final ClientboundRemoveEntitiesPacket packet) {
        for (final int id : packet.getEntityIds()) entityIdToData.remove(id);
    }

    private void packetReceived (final ClientboundEntityPositionSyncPacket packet) {
        final EntityData data = entityIdToData.get(packet.getId());
        if (data == null) return;

        final Vector3d position = packet.getPosition().add(packet.getDeltaMovement());
        final Rotation rotation = new Rotation(packet.getXRot(), packet.getYRot());

        data.position = position;
        data.rotation = rotation;

        bot.listener.dispatch(listener -> listener.onPlayerMoved(data.player, position, rotation));
    }

    private void packetReceived (final ClientboundMoveEntityRotPacket packet) {
        final EntityData data = entityIdToData.get(packet.getEntityId());
        if (data == null) return;

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());
        data.rotation = rotation;

        bot.listener.dispatch(listener -> listener.onPlayerMoved(data.player, data.position, rotation));
    }

    private void packetReceived (final ClientboundMoveEntityPosPacket packet) {
        final EntityData data = entityIdToData.get(packet.getEntityId());
        if (data == null) return;

        final Vector3d originalPosition = data.position;
        final Vector3d newPosition = originalPosition.add(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());

        data.position = newPosition;

        bot.listener.dispatch(listener -> listener.onPlayerMoved(data.player, newPosition, data.rotation));
    }

    private void packetReceived (final ClientboundMoveEntityPosRotPacket packet) {
        final EntityData data = entityIdToData.get(packet.getEntityId());
        if (data == null) return;

        final Vector3d originalPosition = data.position;
        final Vector3d newPosition = originalPosition.add(packet.getMoveX(), packet.getMoveY(), packet.getMoveZ());

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());

        data.position = newPosition;
        data.rotation = rotation;

        bot.listener.dispatch(listener -> listener.onPlayerMoved(data.player, position, rotation));
    }

    // for now this is used in CorePlugin when placing the command block
    private void handleHeightLimit () {
        final double y = position.getY();
        final int minY = bot.world.minY;
        final int maxY = bot.world.maxY;

        if (y <= maxY && y >= minY) {
            if (isGoingDownFromHeightLimit) {
                isGoingDownFromHeightLimit = false;

                bot.listener.dispatch(listener -> listener.onPositionChange(position));
            }

            return;
        }

        isGoingDownFromHeightLimit = true;

        if (y > maxY + 500 || y < minY) {
            if ((System.currentTimeMillis() - tpCommandCooldownTime) < 400) return;

            tpCommandCooldownTime = System.currentTimeMillis();

            final StringBuilder command = new StringBuilder();

            if (bot.serverFeatures.hasEssentials) command.append("essentials:");
            command.append(String.format("tp ~ %s ~", maxY));

            bot.chat.sendCommandInstantly(command.toString());

            return;
        }

        final Vector3d newPosition = Vector3d.from(
                position.getX(),
                MathUtilities.clamp(position.getY() - 2, maxY, position.getY()),
                position.getZ()
        );

        position = newPosition;

        bot.session.send(new ServerboundMovePlayerPosPacket(
                false,
                false,
                newPosition.getX(),
                newPosition.getY(),
                newPosition.getZ()
        ));
    }

    public Vector3d getPlayerPosition (final String playerName) {
        for (final EntityData data : entityIdToData.values()) {
            if (data.player.profile.getName().equals(playerName)) return data.position;
        }

        return null;
    }

    public Rotation getPlayerRotation (final String playerName) {
        for (final EntityData data : entityIdToData.values()) {
            if (data.player.profile.getName().equals(playerName)) return data.rotation;
        }

        return null;
    }
}
