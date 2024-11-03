package me.chayapak1.chomens_bot.plugins;

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
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.Rotation;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

// some part of the code used to be in a test plugin but i thought it would be useful in the future so i moved it here
public class PositionPlugin extends Bot.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public Vector3i position = Vector3i.from(0, 0, 0);

    public boolean isGoingDownFromHeightLimit = false; // cool variable name

    private final Map<Integer, PlayerEntry> entityIdMap = new HashMap<>();
    private final Map<Integer, Vector3f> positionMap = new HashMap<>();
    private final Map<Integer, Rotation> rotationMap = new HashMap<>();

    public PositionPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

        // notchian clients also does this, sends the position packet every second
        bot.executor.scheduleAtFixedRate(() -> {
            if (isGoingDownFromHeightLimit) return;

            bot.session.send(new ServerboundMovePlayerPosPacket(
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
        bot.session.send(new ServerboundAcceptTeleportationPacket(packet.getTeleportId()));

        position = Vector3i.from(packet.getX(), packet.getY(), packet.getZ());
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
        final int y = position.getY();
        final int minY = bot.world.minY;
        final int maxY = bot.world.maxY;

        if (y < maxY) {
            if (isGoingDownFromHeightLimit) {
                isGoingDownFromHeightLimit = false;

                for (Listener listener : listeners) { listener.positionChange(position); }
            }

            return;
        }

        isGoingDownFromHeightLimit = true;

        final Vector3i newPosition = Vector3i.from(
                position.getX(),
                position.getY() - 2,
                position.getZ()
        );

        position = newPosition;

        if (position.getY() > maxY + 500 || position.getY() < minY) {
            String command = "/";

            if (bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.ESSENTIALS)) command += "essentials:";

            command += String.format("tp ~ %s ~", maxY - 1);

            bot.chat.send(command);

            return;
        }

        bot.session.send(new ServerboundMovePlayerPosPacket(
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
        public void positionChange (Vector3i position) {}
        public void playerMoved (PlayerEntry player, Vector3f position, Rotation rotation) {}
    }
}
