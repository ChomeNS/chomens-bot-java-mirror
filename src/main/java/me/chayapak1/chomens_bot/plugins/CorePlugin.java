package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonSyntaxException;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.selfCare.SelfData;
import me.chayapak1.chomens_bot.util.MathUtilities;
import me.chayapak1.chomens_bot.util.StringUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.cloudburstmc.math.vector.Vector3d;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.Hand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponent;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentTypes;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponents;
import org.geysermc.mcprotocollib.protocol.data.game.level.block.CommandBlockMode;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CorePlugin implements Listener {
    private static final int MAX_PENDING_COMMANDS = 256 * 3;
    public static final int COMMAND_BLOCK_ID = 425;

    private final Bot bot;

    public volatile boolean ready = false;

    public final Vector3i fromSize;
    public Vector3i toSize;

    public Vector3i from;
    public Vector3i to;

    public volatile Vector3i block = null;

    public final AtomicInteger index = new AtomicInteger();

    public final Queue<String> placeBlockQueue = new ConcurrentLinkedQueue<>();

    public final Queue<String> pendingCommands = new ConcurrentLinkedQueue<>();

    public final AtomicInteger commandsPerTick = new AtomicInteger();
    public final AtomicInteger commandsPerSecond = new AtomicInteger();

    private final AtomicInteger positionChangesPerSecond = new AtomicInteger(0);

    private boolean exists = false;

    private boolean shouldRefill = false;

    public CorePlugin (final Bot bot) {
        this.bot = bot;

        this.fromSize = Vector3i.from(
                0,
                -64,
                0
        );
        this.toSize = Vector3i.from(
                15,
                -64,
                15
        );

        if (hasRateLimit() && hasReset()) {
            bot.executor.scheduleAtFixedRate(
                    () -> commandsPerSecond.set(0),
                    0,
                    bot.options.coreRateLimit.reset,
                    TimeUnit.MILLISECONDS
            );
        }

        bot.listener.addListener(this);
    }

    @Override
    public void onTick () {
        if (!pendingCommands.isEmpty() && exists && hasEnoughPermissions()) {
            // people that pre-order on TEMU application. Shop Like A Billionaire!!!
            if (pendingCommands.size() > MAX_PENDING_COMMANDS) {
                pendingCommands.clear();
            } else {
                for (final String pendingCommand : pendingCommands) {
                    forceRun(pendingCommand);
                }

                pendingCommands.clear();
            }
        }

        if (placeBlockQueue.size() > 300) {
            placeBlockQueue.clear();
            return;
        }

        final String command = placeBlockQueue.poll();

        if (command == null) return;

        forceRunPlaceBlock(command);
    }

    @Override
    public void onLocalTick () {
        if (commandsPerTick.get() > 0) commandsPerTick.decrementAndGet();
    }

    @Override
    public void onLocalSecondTick () {
        resizeTick();
    }

    @Override
    public void onSecondTick () {
        checkCoreTick();

        exists = isCoreExists();

        if (shouldRefill) {
            refill(false);
            shouldRefill = false;
        }

        positionChangesPerSecond.set(0);
    }

    public boolean hasRateLimit () {
        return bot.options.coreRateLimit.limit > 0;
    }

    public boolean hasReset () {
        return bot.options.coreRateLimit.reset > 0;
    }

    public boolean isRateLimited () {
        return commandsPerSecond.get() > bot.options.coreRateLimit.limit;
    }

    private void forceRun (String command) {
        // if the core isn't ready yet, it is still pretty useless
        // to still run the command, even if forced.
        if (!ready || command.length() > 32767) return;

        commandsPerTick.incrementAndGet();

        if (!bot.serverFeatures.hasNamespaces) command = StringUtilities.removeNamespace(command);

        if (bot.serverFeatures.hasExtras) {
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

        incrementBlock(0);
    }

    public void run (final String command) {
        if (pendingCommands.size() <= MAX_PENDING_COMMANDS && (!exists || !hasEnoughPermissions())) {
            // Add to TEMU shopping cart
            pendingCommands.add(command);
            return;
        }

        if (!ready || command.length() > 32767) return;

        if (bot.options.useCore) {
            if (bot.options.useCorePlaceBlock) {
                runPlaceBlock(command);
                return;
            }

            if (isRateLimited() && hasRateLimit()) return;

            forceRun(command);

            if (hasRateLimit()) commandsPerSecond.incrementAndGet();
        } else if (command.length() < 256) {
            bot.chat.send("/" + command);
        }
    }

    // thanks chipmunk for this "new" tellraw method which is pretty much used by everyone now
    public CompletableFuture<Component> runTracked (final String command) {
        if (!ready || command.length() > 32767) return null;

        if (!bot.options.useCore) return null;

        if (bot.options.useCorePlaceBlock) {
            runPlaceBlock(command);
            return null;
        }

        final Vector3i coreBlock = block.clone();

        run(command);

        final CompletableFuture<Component> trackedFuture = new CompletableFuture<>();

        final CompletableFuture<String> future = bot.query.block(false, coreBlock, "LastOutput", true);

        future.thenApply(output -> {
            if (output == null) return null;

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

    public void runPlaceBlock (final String command) {
        try {
            placeBlockQueue.add(command);
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    public void forceRunPlaceBlock (String command) {
        if (!ready || !bot.options.useCore) return;

        if (!bot.serverFeatures.hasNamespaces) command = StringUtilities.removeNamespace(command);

        final NbtMapBuilder blockEntityTagBuilder = NbtMap.builder();

        blockEntityTagBuilder.putString("id", "minecraft:command_block");
        blockEntityTagBuilder.putString("Command", command);
        blockEntityTagBuilder.putByte("auto", (byte) 1);
        blockEntityTagBuilder.putByte("TrackOutput", (byte) 1);

        final NbtMap blockEntityTag = blockEntityTagBuilder.build();

        final Map<DataComponentType<?>, DataComponent<?, ?>> map = new HashMap<>();

        // is there a better way to put these data component types into the map?

        map.put(
                DataComponentTypes.BLOCK_ENTITY_DATA,
                DataComponentTypes.BLOCK_ENTITY_DATA
                        .getDataComponentFactory()
                        .create(
                                DataComponentTypes.BLOCK_ENTITY_DATA,
                                blockEntityTag
                        )
        );

        try {
            final Component customName = GsonComponentSerializer.gson().deserialize(bot.config.core.customName);

            map.put(
                    DataComponentTypes.CUSTOM_NAME,
                    DataComponentTypes.CUSTOM_NAME
                            .getDataComponentFactory()
                            .create(
                                    DataComponentTypes.CUSTOM_NAME,
                                    customName
                            )
            );
        } catch (final JsonSyntaxException e) {
            bot.logger.error("Error while parsing the core's custom name into Component! You might have an invalid syntax in the custom name.");
            bot.logger.error(e);
        }

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
                                COMMAND_BLOCK_ID,
                                64,
                                dataComponents
                        )
                )
        );
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, false, 1));

        if (!bot.options.useCorePlaceBlock) {
            bot.executor.schedule(() -> session.send(
                    new ServerboundSetCreativeModeSlotPacket(
                            (short) 36,
                            null
                    )
            ), 50 * 2, TimeUnit.MILLISECONDS);
        }
    }

    private void resizeTick () {
        if (!ready) return;

        // fixes a bug where the block positions are more than the ones in from and to
        if (!isCore(block)) recalculateRelativePositions();

        final Vector3i oldSize = toSize;

        final int x = toSize.getX();
        int y = bot.world.minY;
        final int z = toSize.getZ();

        while (commandsPerTick.get() > 16 * 16) {
            y++;
            commandsPerTick.getAndAdd(-(16 * 16));
        }

        y = Math.min(y, bot.world.maxY);

        toSize = Vector3i.from(x, y, z);

        if (oldSize.getY() != toSize.getY()) {
            recalculateRelativePositions();
            refill(false);
        }
    }

    public boolean hasEnoughPermissions () {
        final SelfData data = bot.selfCare.data;

        if (data == null) return false;

        return data.permissionLevel >= 2 && data.gameMode == GameMode.CREATIVE;
    }

    public boolean isCoreComplete () {
        if (!ready) return false;

        for (int y = from.getY(); y <= to.getY(); y++) {
            for (int z = from.getZ(); z <= to.getZ(); z++) {
                for (int x = from.getX(); x <= to.getX(); x++) {
                    final int block = bot.world.getBlock(x, y, z);

                    if (!isCommandBlockState(block)) return false;
                }
            }
        }

        return true;
    }

    // not to be confused with the one above, where it checks if
    // every single block in that core is a command block,
    // this one just only checks if the core exists, meaning
    // that even if the core has only 1 out of 256 blocks,
    // it will still return true
    public boolean isCoreExists () {
        if (!ready) return false;

        for (int y = from.getY(); y <= to.getY(); y++) {
            for (int z = from.getZ(); z <= to.getZ(); z++) {
                for (int x = from.getX(); x <= to.getX(); x++) {
                    final int block = bot.world.getBlock(x, y, z);

                    if (isCommandBlockState(block)) return true;
                }
            }
        }

        return false;
    }

    private void checkCoreTick () {
        if (!isCoreComplete()) shouldRefill = true;
    }

    private boolean isCommandBlockState (final int blockState) {
        return
                (blockState >= 8690 && blockState <= 8701) // command block
                        || (blockState >= 13550 && blockState <= 13561) // chain command block
                        || (blockState >= 13538 && blockState <= 13549); // repeating command block
    }

    // ported from chomens bot js, which is originally from smp
    private boolean isCore (final Vector3i position) {
        return
                position.getX() >= from.getX() && position.getX() <= to.getX() &&
                        position.getY() >= from.getY() && position.getY() <= to.getY() &&
                        position.getZ() >= from.getZ() && position.getZ() <= to.getZ();
    }

    private void incrementBlock (final int times) {
        final int currentIndex = index.get();

        final int x = from.getX() + (currentIndex & 15);
        final int z = from.getZ() + ((currentIndex >> 4) & 15);
        final int y = (currentIndex >> 8) + bot.world.minY;

        block = Vector3i.from(x, y, z);

        index.set((currentIndex + 1) % (256 * Math.max(1, to.getY() - from.getY())));

        if (times <= 256 && !isCommandBlockState(bot.world.getBlock(x, y, z))) {
            incrementBlock(times + 1);
        }
    }

    @Override
    public void onPositionChange (final Vector3d position) {
        if (bot.position.isGoingDownFromHeightLimit) return;

        positionChangesPerSecond.incrementAndGet();

        final int coreChunkPosX = from == null ? -1 : (int) Math.floor((double) from.getX() / 16);
        final int coreChunkPosZ = from == null ? -1 : (int) Math.floor((double) from.getZ() / 16);

        final int botChunkPosX = (int) Math.floor(bot.position.position.getX() / 16);
        final int botChunkPosZ = (int) Math.floor(bot.position.position.getZ() / 16);

        // only relocate the core when it really needs to
        if (
                from == null || to == null ||
                        Math.abs(botChunkPosX - coreChunkPosX) >= bot.world.simulationDistance ||
                        Math.abs(botChunkPosZ - coreChunkPosZ) >= bot.world.simulationDistance
        ) {
            // also clear out the old core
            final String deleteCommand;
            if (from != null && to != null) {
                deleteCommand = String.format(
                        "minecraft:fill %d %d %d %d %d %d air",

                        from.getX(),
                        from.getY(),
                        from.getZ(),

                        to.getX(),
                        to.getY(),
                        to.getZ()
                );
            } else {
                deleteCommand = null;
            }

            reset();
            refill(false);

            runPlaceBlock(deleteCommand);
        }

        if (!ready) {
            ready = true;

            reset();
            refill();

            bot.listener.dispatch(Listener::onCoreReady);
        }
    }

    @Override
    public void onWorldChanged (final String dimension) {
        reset();
        refill();
    }

    @Override
    public void connected (final ConnectedEvent event) {
        // in case we don't have friends like obd and/or hbot (and other bots that turn this gamerule off)
        pendingCommands.add("minecraft:gamerule commandBlockOutput false");
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        ready = false;
        exists = false;
    }

    public void recalculateRelativePositions () {
        final int botChunkPosX = (int) Math.floor(bot.position.position.getX() / 16);
        final int botChunkPosZ = (int) Math.floor(bot.position.position.getZ() / 16);

        from = Vector3i.from(
                fromSize.getX() + botChunkPosX * 16,
                MathUtilities.clamp(fromSize.getY(), bot.world.minY, bot.world.maxY),
                fromSize.getZ() + botChunkPosZ * 16
        );

        to = Vector3i.from(
                toSize.getX() + botChunkPosX * 16,
                MathUtilities.clamp(toSize.getY(), bot.world.minY, bot.world.maxY),
                toSize.getZ() + botChunkPosZ * 16
        );
    }

    public void reset () {
        recalculateRelativePositions();

        block = Vector3i.from(from);
        index.set(0);
    }

    public void refill () { refill(true); }

    public void refill (final boolean force) {
        if (!ready) return;

        final Map<Integer, Boolean> refilledMap = new HashMap<>();

        final String customName = bot.options.useSNBTComponents ?
                String.format("{CustomName:%s}", bot.config.core.customName) :
                String.format(
                        "{CustomName:'%s'}",
                        bot.config.core.customName
                                .replace("\\", "\\\\")
                                .replace("'", "\\'")
                );

        for (int y = from.getY(); y <= to.getY(); y++) {
            for (int z = from.getZ(); z <= to.getZ(); z++) {
                for (int x = from.getX(); x <= to.getX(); x++) {
                    final int block = bot.world.getBlock(x, y, z);

                    final Boolean refilled = refilledMap.get(y);

                    if (
                            (!force && isCommandBlockState(block)) ||
                                    (refilled != null && refilled)
                    ) continue;

                    final boolean useChat = positionChangesPerSecond.get() > 10;

                    // bot becomes ChipmunkMod user when using chat
                    final String command = String.format(
                            "%sfill %d %d %d %d %d %d command_block%s",

                            useChat ? "" : "minecraft:",

                            from.getX(),
                            y,
                            from.getZ(),

                            to.getX(),
                            y,
                            to.getZ(),

                            useChat ? "" : customName
                    );

                    if (useChat) bot.chat.sendCommandInstantly(command);
                    else if (isCoreExists()) run(command);
                    else runPlaceBlock(command);

                    refilledMap.put(y, true);
                }
            }
        }

        if (refilledMap.containsValue(true)) {
            bot.listener.dispatch(Listener::onCoreRefilled);
        }
    }
}
