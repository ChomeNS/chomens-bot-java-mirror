package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.data.entity.Rotation;
import me.chayapak1.chomens_bot.util.MathUtilities;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3f;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
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
public class PositionPlugin extends Bot.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public Vector3d position = Vector3d.from(0, 0, 0);

    public boolean isGoingDownFromHeightLimit = false; // cool variable name

    private final Map<Integer, PlayerEntry> entityIdMap = new HashMap<>();
    private final Map<Integer, Vector3f> positionMap = new HashMap<>();
    private final Map<Integer, Rotation> rotationMap = new HashMap<>();

    public PositionPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

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

        bot.tick.addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                handleHeightLimit();
            }
        });
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundPlayerPositionPacket) packetReceived((ClientboundPlayerPositionPacket) packet);
        else if (packet instanceof ClientboundMoveEntityRotPacket) packetReceived((ClientboundMoveEntityRotPacket) packet);
        else if (packet instanceof ClientboundMoveEntityPosPacket) packetReceived((ClientboundMoveEntityPosPacket) packet);
        else if (packet instanceof ClientboundMoveEntityPosRotPacket) packetReceived((ClientboundMoveEntityPosRotPacket) packet);
        else if (packet instanceof ClientboundAddEntityPacket) packetReceived((ClientboundAddEntityPacket) packet);
        else if (packet instanceof ClientboundRemoveEntitiesPacket) packetReceived((ClientboundRemoveEntitiesPacket) packet);
    }

    public void packetReceived (ClientboundPlayerPositionPacket packet) {
        if (bot.session != null) bot.session.send(new ServerboundAcceptTeleportationPacket(packet.getId()));

        position = packet.getPosition();

        for (Listener listener : listeners) { listener.positionChange(position); }
    }

    public void packetReceived (ClientboundAddEntityPacket packet) {
        final PlayerEntry entry = bot.players.getEntry(packet.getUuid());

        if (entry == null) return;

        entityIdMap.remove(packet.getEntityId());
        positionMap.remove(packet.getEntityId());
        rotationMap.remove(packet.getEntityId());

        entityIdMap.put(packet.getEntityId(), entry);
        positionMap.put(packet.getEntityId(), Vector3f.from(packet.getX(), packet.getY(), packet.getZ()));
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

        final Vector3f lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());

        final Vector3f position = Vector3f.from(
                packet.getMoveX() + lastPosition.getX(),
                packet.getMoveY() + lastPosition.getY(),
                packet.getMoveZ() + lastPosition.getZ()
        );

        positionMap.put(packet.getEntityId(), position);

        for (Listener listener : listeners) listener.playerMoved(player, position, getPlayerRotation(player.profile.getName()));
    }

    public void packetReceived (ClientboundMoveEntityPosRotPacket packet) {
        final PlayerEntry player = entityIdMap.get(packet.getEntityId());

        if (player == null) return;

        final Vector3f lastPosition = positionMap.get(packet.getEntityId());

        positionMap.remove(packet.getEntityId());
        rotationMap.remove(packet.getEntityId());

        final Vector3f position = Vector3f.from(
                packet.getMoveX() + lastPosition.getX(),
                packet.getMoveY() + lastPosition.getY(),
                packet.getMoveZ() + lastPosition.getZ()
        );

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

            if (bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.ESSENTIALS)) command += "essentials:";

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

    public Vector3f getPlayerPosition (String playerName) {
        int entityId = -1;
        for (Map.Entry<Integer, PlayerEntry> entry : entityIdMap.entrySet()) {
            if (entry.getValue().profile.getName().equals(playerName)) entityId = entry.getKey();
        }

        if (entityId == -1) return null;

        for (Map.Entry<Integer, Vector3f> entry : positionMap.entrySet()) {
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

    public static class Listener {
        public void positionChange (Vector3d position) {}
        public void playerMoved (PlayerEntry player, Vector3f position, Rotation rotation) {}
    }
}
