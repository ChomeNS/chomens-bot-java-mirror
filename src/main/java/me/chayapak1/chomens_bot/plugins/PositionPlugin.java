package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.entity.Rotation;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.MathUtilities;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.*;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.spawn.ClientboundAddEntityPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// some part of the code used to be in a test plugin but i thought it would be useful in the future so i moved it here
public class PositionPlugin extends Bot.Listener implements TickPlugin.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public Vector3d position = Vector3d.from(0, 0, 0);

    public boolean isGoingDownFromHeightLimit = false; // cool variable name

    private final Map<Integer, PlayerEntry> entityIdMap = new HashMap<>();
    private final Map<Integer, Vector3d> positionMap = new HashMap<>();
    private final Map<Integer, Rotation> rotationMap = new HashMap<>();

    public PositionPlugin (Bot bot) {
        this.bot = bot;

        // notchian clients also does this, sends the position packet every second
        bot.executor.scheduleAtFixedRate(() -> {
            if (!bot.loggedIn || isGoingDownFromHeightLimit) return;

            bot.session.send(new ServerboundMovePlayerPosPacket(
                    false,
                    false,
                    position.getX(),
                    position.getY(),
                    position.getZ()
            ));
        }, 0, 1, TimeUnit.SECONDS);

        bot.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onTick() {
        handleHeightLimit();
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerPositionPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundAddEntityPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundRemoveEntitiesPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundMoveEntityRotPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundMoveEntityPosPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundMoveEntityPosRotPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof ClientboundEntityPositionSyncPacket t_packet) packetReceived(t_packet);
    }

    public void packetReceived (ClientboundPlayerPositionPacket packet) {
        if (bot.session != null) bot.session.send(new ServerboundAcceptTeleportationPacket(packet.getId()));

        position = packet.getPosition();

        for (Listener listener : listeners) { listener.positionChange(position); }
    }

    public void packetReceived (ClientboundAddEntityPacket packet) {
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

    public void packetReceived (ClientboundRemoveEntitiesPacket packet) {
        final int[] ids = packet.getEntityIds();

        for (int id : ids) {
            entityIdMap.remove(id);
            positionMap.remove(id);
            rotationMap.remove(id);
        }
    }

    public void packetReceived (ClientboundEntityPositionSyncPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getId());

        if (player == null) return;

        positionMap.remove(packet.getId());
        rotationMap.remove(packet.getId());

        final Vector3d position = packet.getPosition().add(packet.getDeltaMovement());
        final Rotation rotation = new Rotation(packet.getXRot(), packet.getYRot());

        positionMap.put(packet.getId(), position);
        rotationMap.put(packet.getId(), rotation);

        for (Listener listener : listeners) listener.playerMoved(player, position, rotation);
    }

    public void packetReceived (ClientboundMoveEntityRotPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());

        rotationMap.put(packet.getEntityId(), rotation);

        for (Listener listener : listeners) listener.playerMoved(player, getPlayerPosition(player.profile.getName()), rotation);
    }

    public void packetReceived (ClientboundMoveEntityPosPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Vector3d lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());

        // code ported straight from minecraft
        Vector3d position = lastPosition;

        if (packet.getMoveX() != 0 || packet.getMoveY() != 0 || packet.getMoveZ() != 0) {
            position = position.add(
                    packet.getMoveX() == 0 ? 0 : packet.getMoveX(),
                    packet.getMoveY() == 0 ? 0 : packet.getMoveY(),
                    packet.getMoveZ() == 0 ? 0 : packet.getMoveZ()
            );
        }

        positionMap.put(packet.getEntityId(), position);

        for (Listener listener : listeners) listener.playerMoved(player, position, getPlayerRotation(player.profile.getName()));
    }

    public void packetReceived (ClientboundMoveEntityPosRotPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Vector3d lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());
        rotationMap.remove(packet.getEntityId());

        Vector3d position = lastPosition;

        if (packet.getMoveX() != 0 || packet.getMoveY() != 0 || packet.getMoveZ() != 0) {
            position = position.add(
                    packet.getMoveX() == 0 ? 0 : packet.getMoveX(),
                    packet.getMoveY() == 0 ? 0 : packet.getMoveY(),
                    packet.getMoveZ() == 0 ? 0 : packet.getMoveZ()
            );
        }

        final Rotation rotation = new Rotation(packet.getYaw(), packet.getPitch());

        positionMap.put(packet.getEntityId(), position);
        rotationMap.put(packet.getEntityId(), rotation);

        for (Listener listener : listeners) listener.playerMoved(player, position, rotation);
    }

    // for now this is used in CorePlugin when placing the command block
    private void handleHeightLimit () {
        final double y = position.getY();
        final int minY = bot.world.minY;
        final int maxY = bot.world.maxY;

        if (y <= maxY && y >= minY) {
            if (isGoingDownFromHeightLimit) {
                isGoingDownFromHeightLimit = false;

                for (Listener listener : listeners) { listener.positionChange(position); }
            }

            return;
        }

        isGoingDownFromHeightLimit = true;

        if (y > maxY + 500 || y < minY) {
            String command = "/";

            if (bot.serverFeatures.hasEssentials) command += "essentials:";

            command += String.format("tp ~ %s ~", maxY);

            bot.chat.send(command);

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

    public Vector3d getPlayerPosition (String playerName) {
        int entityId = -1;
        for (Map.Entry<Integer, PlayerEntry> entry : entityIdMap.entrySet()) {
            if (entry.getValue().profile.getName().equals(playerName)) entityId = entry.getKey();
        }

        if (entityId == -1) return null;

        for (Map.Entry<Integer, Vector3d> entry : positionMap.entrySet()) {
            if (entry.getKey() == entityId) return entry.getValue();
        }

        return null;
    }

    public Rotation getPlayerRotation (String playerName) {
        int entityId = -1;
        for (Map.Entry<Integer, PlayerEntry> entry : entityIdMap.entrySet()) {
            if (entry.getValue().profile.getName().equals(playerName)) entityId = entry.getKey();
        }

        if (entityId == -1) return null;

        for (Map.Entry<Integer, Rotation> entry : rotationMap.entrySet()) {
            if (entry.getKey() == entityId) return entry.getValue();
        }

        return null;
    }

    public void addListener (Listener listener) { listeners.add(listener); }

    @SuppressWarnings("unused")
    public interface Listener {
        default void positionChange (Vector3d position) {}
        default void playerMoved (PlayerEntry player, Vector3d position, Rotation rotation) {}
    }
}
