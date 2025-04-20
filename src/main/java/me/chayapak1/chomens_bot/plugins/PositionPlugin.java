package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.entity.Rotation;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.MathUtilities;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// some part of the code used to be in a test plugin but i thought it would be useful in the future so i moved it here
public class PositionPlugin implements Listener {
    private final Bot bot;

    public Vector3d position = Vector3d.from(0, 0, 0);

    public boolean isGoingDownFromHeightLimit = false; // cool variable name

    private long tpCommandCooldownTime = 0;

    private final Map<Integer, PlayerEntry> entityIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, Vector3d> positionMap = new ConcurrentHashMap<>();
    private final Map<Integer, Rotation> rotationMap = new ConcurrentHashMap<>();

    public PositionPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    @Override
    public void onTick () {
        handleHeightLimit();
    }

    // notchian clients also does this, where it sends the current position to the server every second
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
        if (packet instanceof final ClientboundPlayerPositionPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundAddEntityPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundRemoveEntitiesPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundMoveEntityRotPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundMoveEntityPosPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundMoveEntityPosRotPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundEntityPositionSyncPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundPlayerPositionPacket packet) {
        bot.session.send(new ServerboundAcceptTeleportationPacket(packet.getId()));

        position = packet.getPosition();

        bot.listener.dispatch(listener -> listener.onPositionChange(position));
    }

    private void packetReceived (final ClientboundAddEntityPacket packet) {
        if (packet.getType() != EntityType.PLAYER) return;

        final PlayerEntry entry = bot.players.getEntry(packet.getUuid());

        if (entry == null) return;

        entityIdMap.remove(packet.getEntityId());
        positionMap.remove(packet.getEntityId());
        rotationMap.remove(packet.getEntityId());

        entityIdMap.put(packet.getEntityId(), entry);
        positionMap.put(packet.getEntityId(), Vector3d.from(packet.getX(), packet.getY(), packet.getZ()));
        rotationMap.put(packet.getEntityId(), new Rotation(packet.getYaw(), packet.getPitch()));
    }

    private void packetReceived (final ClientboundRemoveEntitiesPacket packet) {
        final int[] ids = packet.getEntityIds();

        for (final int id : ids) {
            entityIdMap.remove(id);
            positionMap.remove(id);
            rotationMap.remove(id);
        }
    }

    private void packetReceived (final ClientboundEntityPositionSyncPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getId());

        if (player == null) return;

        positionMap.remove(packet.getId());
        rotationMap.remove(packet.getId());

        final Vector3d position = packet.getPosition().add(packet.getDeltaMovement());
        final Rotation rotation = new Rotation(packet.getXRot(), packet.getYRot());

        positionMap.put(packet.getId(), position);
        rotationMap.put(packet.getId(), rotation);

        bot.listener.dispatch(listener -> listener.onPlayerMoved(player, position, rotation));
    }

    private void packetReceived (final ClientboundMoveEntityRotPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());

        rotationMap.put(packet.getEntityId(), rotation);

        bot.listener.dispatch(listener -> listener.onPlayerMoved(player, getPlayerPosition(player.profile.getName()), rotation));
    }

    private void packetReceived (final ClientboundMoveEntityPosPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Vector3d lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());

        // code ported straight from minecraft
        final Vector3d position;

        if (packet.getMoveX() != 0 || packet.getMoveY() != 0 || packet.getMoveZ() != 0) {
            position = lastPosition.add(
                    packet.getMoveX() == 0 ? 0 : packet.getMoveX(),
                    packet.getMoveY() == 0 ? 0 : packet.getMoveY(),
                    packet.getMoveZ() == 0 ? 0 : packet.getMoveZ()
            );
        } else {
            position = lastPosition;
        }

        positionMap.put(packet.getEntityId(), position);

        bot.listener.dispatch(listener -> listener.onPlayerMoved(player, position, getPlayerRotation(player.profile.getName())));
    }

    private void packetReceived (final ClientboundMoveEntityPosRotPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Vector3d lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());
        rotationMap.remove(packet.getEntityId());

        final Vector3d position;

        if (packet.getMoveX() != 0 || packet.getMoveY() != 0 || packet.getMoveZ() != 0) {
            position = lastPosition.add(
                    packet.getMoveX() == 0 ? 0 : packet.getMoveX(),
                    packet.getMoveY() == 0 ? 0 : packet.getMoveY(),
                    packet.getMoveZ() == 0 ? 0 : packet.getMoveZ()
            );
        } else {
            position = lastPosition;
        }

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());

        positionMap.put(packet.getEntityId(), position);
        rotationMap.put(packet.getEntityId(), rotation);

        bot.listener.dispatch(listener -> listener.onPlayerMoved(player, position, rotation));
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
        int entityId = -1;
        for (final Map.Entry<Integer, PlayerEntry> entry : entityIdMap.entrySet()) {
            if (entry.getValue().profile.getName().equals(playerName)) entityId = entry.getKey();
        }

        if (entityId == -1) return null;

        for (final Map.Entry<Integer, Vector3d> entry : positionMap.entrySet()) {
            if (entry.getKey() == entityId) return entry.getValue();
        }

        return null;
    }

    public Rotation getPlayerRotation (final String playerName) {
        int entityId = -1;
        for (final Map.Entry<Integer, PlayerEntry> entry : entityIdMap.entrySet()) {
            if (entry.getValue().profile.getName().equals(playerName)) entityId = entry.getKey();
        }

        if (entityId == -1) return null;

        for (final Map.Entry<Integer, Rotation> entry : rotationMap.entrySet()) {
            if (entry.getKey() == entityId) return entry.getValue();
        }

        return null;
    }
}
