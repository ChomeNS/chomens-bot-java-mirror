package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.level.block.BlockChangeEntry;
import com.github.steveice10.mc.protocol.data.game.level.block.CommandBlockMode;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundBlockUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSectionBlocksUpdatePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundTagQueryPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundBlockEntityTagQuery;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.MathUtilities;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CorePlugin extends PositionPlugin.Listener {
    private final Bot bot;

    private final List<Listener> listeners = new ArrayList<>();

    public boolean ready = false;

    private ScheduledFuture<?> refillTask;

    public final Vector3i fromSize;
    public final Vector3i toSize;

    public Vector3i from;
    public Vector3i to;

    public Vector3i block = null;

    private int nextTransactionId = 0;
    private final Map<Integer, CompletableFuture<CompoundTag>> transactions = new HashMap<>();

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

                refillTask.cancel(true);

                reset();
            }

            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ClientboundTagQueryPacket) CorePlugin.this.packetReceived((ClientboundTagQueryPacket) packet);
                else if (packet instanceof ClientboundBlockUpdatePacket) CorePlugin.this.packetReceived((ClientboundBlockUpdatePacket) packet);
                else if (packet instanceof ClientboundSectionBlocksUpdatePacket) CorePlugin.this.packetReceived((ClientboundSectionBlocksUpdatePacket) packet);
                else if (packet instanceof ClientboundLevelChunkWithLightPacket) CorePlugin.this.packetReceived((ClientboundLevelChunkWithLightPacket) packet);
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
            if (isRateLimited() && hasRateLimit()) return;

            forceRun(command);

            if (hasRateLimit()) commandsPerSecond++;
        } else if (command.length() < 256) {
            bot.chat.send("/" + command);
        }
    }

    public CompletableFuture<CompoundTag> runTracked (String command) {
        final Vector3i beforeBlock = block.clone();

        run(command);

        if (!bot.options.useCore) return null;

        final int transactionId = nextTransactionId++;

        // promises are renamed to future lmao
        final CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final Runnable afterTick = () -> bot.session.send(new ServerboundBlockEntityTagQuery(transactionId, beforeBlock));

        bot.executor.schedule(afterTick, 50, TimeUnit.MILLISECONDS);

        return future;
    }

    public void runPlaceBlock (String command) {
        if (!ready || !bot.options.useCore) return;

        final CompoundTag tag = new CompoundTag("");
        final CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
        blockEntityTag.put(new StringTag("Command", command));
        blockEntityTag.put(new ByteTag("auto", (byte) 1));
        blockEntityTag.put(new ByteTag("TrackOutput", (byte) 1));
        tag.put(blockEntityTag);

        final Vector3i temporaryBlockPosition = Vector3i.from(
                bot.position.position.getX(),
                bot.position.position.getY() - 1,
                bot.position.position.getZ()
        );

        final Session session = bot.session;
        session.send(
                new ServerboundSetCreativeModeSlotPacket(
                        36,
                        new ItemStack(
                                bot.serverPluginsManager.hasPlugin(ServerPluginsManagerPlugin.EXTRAS) ?
                                        492 /* repeating command block id */ :
                                        373 /* command block id */,
                                64,
                                tag
                        )
                )
        );
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, 1));
    }

    public void packetReceived (ClientboundTagQueryPacket packet) {
        transactions.get(packet.getTransactionId()).complete(packet.getNbt());
    }

    public void packetReceived (ClientboundBlockUpdatePacket packet) {
        final BlockChangeEntry entry = packet.getEntry();

        final Vector3i position = entry.getPosition();

        if (isCore(position) && !isCommandBlockUpdate(entry.getBlock())) shouldRefill = true;
    }

    public void packetReceived (ClientboundSectionBlocksUpdatePacket packet) {
        final BlockChangeEntry[] entries = packet.getEntries();

        boolean willRefill = false;

        for (BlockChangeEntry entry : entries) {
            final Vector3i position = entry.getPosition();

            if (isCore(position) && !isCommandBlockUpdate(entry.getBlock())) willRefill = true;
        }

        if (willRefill) shouldRefill = true;
    }

    public void packetReceived (ClientboundLevelChunkWithLightPacket packet) {
        shouldRefill = true; // worst fix
    }

    private boolean isCommandBlockUpdate(int blockState) {
        return
                // command block
                (
                        blockState >= 7902 &&
                                blockState <= 7913
                ) ||

                        // chain command block
                        (
                                blockState >= 12371 &&
                                        blockState <= 12382
                        ) ||

                        // repeating command block
                        (
                                blockState >= 12359 &&
                                        blockState <= 12370
                        );
    }

    // ported from chomens bot js
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
    public void positionChange (Vector3i position) {
        from = Vector3i.from(
                fromSize.getX() + bot.position.position.getX(),
                MathUtilities.clamp(fromSize.getY(), bot.world.minY, bot.world.maxY),
                fromSize.getZ() + bot.position.position.getZ()
        );

        to = Vector3i.from(
                toSize.getX() + bot.position.position.getX(),
                MathUtilities.clamp(toSize.getY(), bot.world.minY, bot.world.maxY),
                toSize.getZ() + bot.position.position.getZ()
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
