package me.chayapak1.chomens_bot.plugins;

import com.google.gson.JsonSyntaxException;
import me.chayapak1.chomens_bot.Bot;
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
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.object.Direction;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CorePlugin
        extends Bot.Listener
        implements PositionPlugin.Listener, WorldPlugin.Listener, TickPlugin.Listener
{
    public static final int COMMAND_BLOCK_ID = 418;

    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public boolean ready = false;

    private ScheduledFuture<?> refillTask;

    public final Vector3i fromSize;
    public Vector3i toSize;

    public Vector3i from;
    public Vector3i to;

    public Vector3i block = null;

    public final ConcurrentLinkedQueue<String> placeBlockQueue = new ConcurrentLinkedQueue<>();

    private int commandsPerTick = 0;
    private int commandsPerSecond = 0;

    private boolean shouldRefill = false;

    public CorePlugin (Bot bot) {
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

        bot.position.addListener(this);

        if (hasRateLimit() && hasReset()) {
            bot.executor.scheduleAtFixedRate(
                    () -> commandsPerSecond = 0,
                    0,
                    bot.options.coreRateLimit.reset,
                    TimeUnit.MILLISECONDS
            );
        }

        bot.addListener(this);
        bot.world.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onTick () {
        if (commandsPerTick > 0) commandsPerTick--;

        try {
            if (placeBlockQueue.size() > 300) {
                placeBlockQueue.clear();
                return;
            }

            final String command = placeBlockQueue.poll();

            if (command == null) return;

            forceRunPlaceBlock(command);
        } catch (Exception e) {
            bot.logger.error(e);
        }
    }

    @Override
    public void onSecondTick () {
        checkCoreTick();

        resizeTick();

        if (!shouldRefill) return;

        refill(false);
        shouldRefill = false;
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
        // if the core isn't ready yet, it is still pretty useless
        // to still run the command, even if forced.
        if (!ready || command.length() > 32767) return;

        commandsPerTick++;

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

        final Vector3i coreBlock = block.clone();

        run(command);

        final CompletableFuture<Component> trackedFuture = new CompletableFuture<>();

        final CompletableFuture<String> future = bot.query.block(coreBlock, "LastOutput");

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

    public void runPlaceBlock (String command) {
        try {
            placeBlockQueue.add(command);
        } catch (Exception e) {
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
        } catch (JsonSyntaxException e) {
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
    }

    private void resizeTick () {
        if (!ready) return;

        // fixes a bug where the block positions are more than the ones in from and to
        if (!isCore(block)) reset();

        final Vector3i oldSize = toSize;

        int x = toSize.getX();
        int y = -64;
        int z = toSize.getZ();

        while (commandsPerTick > (16 * 16)) {
            y++;
            commandsPerTick -= 16 * 16;
        }

        toSize = Vector3i.from(x, y, z);

        if (oldSize.getY() != toSize.getY()) {
            recalculateRelativePositions();

            // this will be run just after this function finishes, since it runs in the same interval
            shouldRefill = true;
        }
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

    private void checkCoreTick () {
        if (!isCoreComplete()) shouldRefill = true;
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
        int x = block.getX() + 1;
        int y = block.getY();
        int z = block.getZ();

        if (x > to.getX()) {
            x = from.getX();
            z++;
            if (z > to.getZ()) {
                z = from.getZ();
                y++;
                if (y > to.getY()) {
                    y = from.getY();
                }
            }
        }

        block = Vector3i.from(x, y, z);
    }

    @Override
    public void positionChange (Vector3d position) {
        if (bot.position.isGoingDownFromHeightLimit) return;

        final int coreChunkPosX = from == null ? -1 : (int) Math.floor((double) from.getX() / 16);
        final int coreChunkPosZ = from == null ? -1 : (int) Math.floor((double) from.getZ() / 16);

        final int botChunkPosX = (int) Math.floor(bot.position.position.getX() / 16);
        final int botChunkPosZ = (int) Math.floor(bot.position.position.getZ() / 16);

        // only relocate the core when it really needs to
        if (
                from == null || to == null ||
                        Math.abs(botChunkPosX - coreChunkPosX) > bot.world.simulationDistance ||
                        Math.abs(botChunkPosZ - coreChunkPosZ) > bot.world.simulationDistance
        ) {
            reset();
            refill(false);
        }

        if (!ready) {
            ready = true;

            refillTask = bot.executor.scheduleAtFixedRate(this::refill, 0, bot.config.core.refillInterval, TimeUnit.MILLISECONDS);
            for (Listener listener : listeners) listener.coreReady();
        }
    }

    @Override
    public void worldChanged (String dimension) {
        reset();
        refill();
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        ready = false;

        refillTask.cancel(false);

        reset();
    }

    public void recalculateRelativePositions() {
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
    }

    public void refill () { refill(true); }
    public void refill (boolean force) {
        if (!ready) return;

        final Map<Integer, Boolean> refilledMap = new HashMap<>();

        for (int y = from.getY(); y <= to.getY(); y++) {
            for (int z = from.getZ(); z <= to.getZ(); z++) {
                for (int x = from.getX(); x <= to.getX(); x++) {
                    final int block = bot.world.getBlock(x, y, z);

                    final Boolean refilled = refilledMap.get(y);

                    if (
                            (!force && isCommandBlockState(block)) ||
                                    (refilled != null && refilled)
                    ) continue;

                    final String command = String.format(
                            "minecraft:fill %s %s %s %s %s %s minecraft:command_block{CustomName:'%s'}",

                            from.getX(),
                            y,
                            from.getZ(),

                            to.getX(),
                            y,
                            to.getZ(),

                            bot.config.core.customName
                    );

                    //        bot.chat.send(command);

                    runPlaceBlock(command);

                    refilledMap.put(y, true);
                }
            }
        }

        if (refilledMap.containsValue(true)) {
            for (Listener listener : listeners) listener.coreRefilled();
        }
    }

    public interface Listener {
        default void coreReady () {}
        default void coreRefilled () {}
    }

    public void addListener (Listener listener) { listeners.add(listener); }
}
