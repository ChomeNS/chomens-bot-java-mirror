package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockChangeEntry;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.CommandBlockMode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CorePlugin extends PositionPlugin.Listener {
    public static final int COMMAND_BLOCK_ID = 418;
    public static final int REPEATING_COMMAND_BLOCK_ID = 537;

    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public boolean ready = false;

    private ScheduledFuture<?> refillTask;

    public final Vector3i fromSize;
    public final Vector3i toSize;

    public Vector3i from;
    public Vector3i to;

    public Vector3i block = null;

    public final List<String> placeBlockQueue = Collections.synchronizedList(new ArrayList<>());

    private int commandsPerSecond = 0;

    private boolean shouldRefill = false;

    public CorePlugin (Bot bot) {
        this.bot = bot;

        this.fromSize = Vector3i.from(
                bot.config.core.start.x,
                bot.config.core.start.y,
                bot.config.core.start.z
        );
        this.toSize = Vector3i.from(
                bot.config.core.end.x,
                bot.config.core.end.y,
                bot.config.core.end.z
        );

        bot.position.addListener(this);

        if (hasRateLimit() && hasReset()) {
            bot.executor.scheduleAtFixedRate(
                    () -> commandsPerSecond = 0,
                    0,
                    bot.options.coreRateLimit.reset,
                    TimeUnit.MILLISECONDS
            );
        }

        bot.executor.scheduleAtFixedRate(() -> {
            if (!shouldRefill) return;

            refill();
            shouldRefill = false;
        }, 0, 1, TimeUnit.SECONDS);

        bot.addListener(new Bot.Listener() {
            @Override
            public void disconnected (DisconnectedEvent event) {
                ready = false;

                refillTask.cancel(false);

                reset();
            }

            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ClientboundBlockUpdatePacket) CorePlugin.this.packetReceived((ClientboundBlockUpdatePacket) packet);
                else if (packet instanceof ClientboundSectionBlocksUpdatePacket) CorePlugin.this.packetReceived((ClientboundSectionBlocksUpdatePacket) packet);
                else if (packet instanceof ClientboundLevelChunkWithLightPacket) CorePlugin.this.packetReceived((ClientboundLevelChunkWithLightPacket) packet);
            }
        });

        bot.tick.addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                try {
                    final List<String> clonedQueue = new ArrayList<>(placeBlockQueue);

                    if (clonedQueue.isEmpty()) return;

                    if (clonedQueue.size() > 500) {
                        placeBlockQueue.clear();
                        return;
                    }

                    forceRunPlaceBlock(clonedQueue.getFirst());
                    placeBlockQueue.removeFirst();
                } catch (Exception e) {
                    bot.logger.error(e);
                }
            }
        });
    }

    public boolean hasRateLimit () {
        return bot.options.coreRateLimit.limit > 0;
    }

    public boolean hasReset () {
        return bot.options.coreRateLimit.reset > 0;
    }

    public boolean isRateLimited () {
        return commandsPerSecond > bot.options.coreRateLimit.limit;
    }

    private void forceRun (String command) {
        if (bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.EXTRAS)) {
            bot.session.send(new ServerboundSetCommandBlockPacket(
                    block,
                    command,
                    CommandBlockMode.AUTO,
                    true,
                    false,
                    true
            ));
        } else {
            bot.session.send(new ServerboundSetCommandBlockPacket(
                    block,
                    "",
                    CommandBlockMode.REDSTONE,
                    false,
                    false,
                    false
            ));

            bot.session.send(new ServerboundSetCommandBlockPacket(
                    block,
                    command,
                    CommandBlockMode.REDSTONE,
                    true,
                    false,
                    true
            ));
        }

        incrementBlock();
    }

    public void run (String command) {
        if (!ready || command.length() > 32767) return;

        if (bot.options.useCore) {
            if (bot.options.useCorePlaceBlock) {
                runPlaceBlock(command);
                return;
            }

            if (isRateLimited() && hasRateLimit()) return;

            forceRun(command);

            if (hasRateLimit()) commandsPerSecond++;
        } else if (command.length() < 256) {
            bot.chat.send("/" + command);
        }
    }

    // thanks chipmunk for this new tellraw method
    public CompletableFuture<Component> runTracked (String command) {
        if (!ready || command.length() > 32767) return null;

        if (!bot.options.useCore) return null;

        if (bot.options.useCorePlaceBlock) {
            runPlaceBlock(command);
            return null;
        }

        final Vector3i coreBlock = block;

        run(command);

        final CompletableFuture<Component> trackedFuture = new CompletableFuture<>();

        final CompletableFuture<String> future = bot.query.block(coreBlock, "LastOutput");

        future.thenApplyAsync(output -> {
            if (output == null) return output;

            trackedFuture.complete(
                    Component.join(
                            JoinConfiguration.separator(Component.empty()),
                            GsonComponentSerializer.gson()
                                    .deserialize(output)
                                    .children() // ignores the time
                    )
            );

            return output;
        });

        return trackedFuture;
    }

    public void runPlaceBlock (String command) {
        try {
            placeBlockQueue.add(command);
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    public void forceRunPlaceBlock (String command) {
        if (!ready || !bot.options.useCore) return;

        final NbtMapBuilder blockEntityTagBuilder = NbtMap.builder();

        blockEntityTagBuilder.putString("id", "minecraft:command_block");
        blockEntityTagBuilder.putString("Command", command);
        blockEntityTagBuilder.putByte("auto", (byte) 1);
        blockEntityTagBuilder.putByte("TrackOutput", (byte) 1);
        blockEntityTagBuilder.putString("CustomName", bot.config.core.customName);

        final NbtMap blockEntityTag = blockEntityTagBuilder.build();

        final Map<DataComponentType<?>, DataComponent<?, ?>> map = new HashMap<>();

        map.put(DataComponentType.BLOCK_ENTITY_DATA, DataComponentType.BLOCK_ENTITY_DATA.getDataComponentFactory().create(DataComponentType.BLOCK_ENTITY_DATA, blockEntityTag));

        final DataComponents dataComponents = new DataComponents(map);

        final Vector3i temporaryBlockPosition = Vector3i.from(
                bot.position.position.getX(),
                bot.position.position.getY() - 1,
                bot.position.position.getZ()
        );

        final Session session = bot.session;
        session.send(
                new ServerboundSetCreativeModeSlotPacket(
                        (short) 36,
                        new ItemStack(
                                bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.EXTRAS) ?
                                        REPEATING_COMMAND_BLOCK_ID :
                                        COMMAND_BLOCK_ID,
                                64,
                                dataComponents
                        )
                )
        );
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, false, 1));
    }

    public void packetReceived (ClientboundBlockUpdatePacket packet) {
        final BlockChangeEntry entry = packet.getEntry();

        final Vector3i position = entry.getPosition();

        if (isCore(position) && !isCommandBlockState(entry.getBlock())) shouldRefill = true;
    }

    public void packetReceived (ClientboundSectionBlocksUpdatePacket packet) {
        final BlockChangeEntry[] entries = packet.getEntries();

        for (BlockChangeEntry entry : entries) {
            final Vector3i position = entry.getPosition();

            if (isCore(position) && !isCommandBlockState(entry.getBlock())) {
                shouldRefill = true;
                break;
            }
        }
    }

    public void packetReceived (ClientboundLevelChunkWithLightPacket packet) {
        shouldRefill = true; // worst fix
    }

    private boolean isCommandBlockState (int blockState) {
        return
                // command block
                (
                        blockState >= 8680 &&
                                blockState <= 8686
                ) ||

                        // chain command block
                        (
                                blockState >= 13540 &&
                                        blockState <= 13546
                        ) ||

                        // repeating command block
                        (
                                blockState >= 13534 &&
                                        blockState <= 13540
                        );
    }

    // ported from chomens bot js, which is originally from smp
    private boolean isCore (Vector3i position) {
        return
                position.getX() >= from.getX() && position.getX() <= to.getX() &&
                        position.getY() >= from.getY() && position.getY() <= to.getY() &&
                        position.getZ() >= from.getZ() && position.getZ() <= to.getZ();
    }

    private void incrementBlock () {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();

        x++;

        if (x > to.getX()) {
            x = from.getX();
            z++;
        }

        if (z > to.getZ()) {
            z = from.getZ();
            y++;
        }

        if (y > to.getY()) {
            x = from.getX();
            y = from.getY();
            z = from.getZ();
        }

        block = Vector3i.from(x, y, z);
    }

    @Override
    public void positionChange (Vector3d position) {
        if (bot.position.isGoingDownFromHeightLimit) return;

        from = Vector3i.from(
                (int) (fromSize.getX() + Math.floor(bot.position.position.getX() / 16) * 16),
                MathUtilities.clamp(fromSize.getY(), bot.world.minY, bot.world.maxY),
                (int) (fromSize.getZ() + Math.floor(bot.position.position.getZ() / 16) * 16)
        );

        to = Vector3i.from(
                (int) (toSize.getX() + Math.floor(bot.position.position.getX() / 16) * 16),
                MathUtilities.clamp(toSize.getY(), bot.world.minY, bot.world.maxY),
                (int) (toSize.getZ() + Math.floor(bot.position.position.getZ() / 16) * 16)
        );

        reset();
        refill();

        if (!ready) {
            ready = true;

            refillTask = bot.executor.scheduleAtFixedRate(this::refill, 0, bot.config.core.refillInterval, TimeUnit.MILLISECONDS);
            for (Listener listener : listeners) listener.ready();
        }
    }

    public void reset () {
        block = Vector3i.from(from);
    }

    public void refill () {
        if (!ready) return;

        final String command = String.format(
                "minecraft:fill %s %s %s %s %s %s minecraft:command_block{CustomName:'%s'}",

                from.getX(),
                from.getY(),
                from.getZ(),

                to.getX(),
                to.getY(),
                to.getZ(),

                bot.config.core.customName
        );

//        bot.chat.send(command);

        runPlaceBlock(command);

        for (Listener listener : listeners) listener.refilled();
    }

    public static class Listener {
        public void ready () {}
        public void refilled () {}
    }

    public void addListener (Listener listener) { listeners.add(listener); }
}
