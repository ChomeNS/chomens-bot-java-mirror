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
import lombok.Getter;
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

    @Getter private final List<Listener> listeners = new ArrayList<>();

    @Getter private boolean ready = false;

    private ScheduledFuture<?> refillTask;

    public final Vector3i coreStart;
    public final Vector3i coreEnd;

    public Vector3i origin;
    public Vector3i originEnd;

    public Vector3i relativeCorePosition;

    private int nextTransactionId = 0;
    private final Map<Integer, CompletableFuture<CompoundTag>> transactions = new HashMap<>();

    private final boolean kaboom;

    public CorePlugin (Bot bot) {
        this.bot = bot;
        this.kaboom = bot.options().kaboom();

        this.coreStart = Vector3i.from(
                bot.config().core().start().x(),
                bot.config().core().start().y(),
                bot.config().core().start().z()
        );
        this.coreEnd = Vector3i.from(
                bot.config().core().end().x(),
                bot.config().core().end().y(),
                bot.config().core().end().z()
        );

        this.relativeCorePosition = Vector3i.from(coreStart);

        bot.position().addListener(this);

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

    public void run (String command) {
        if (!ready) return;

        if (bot.options().useCore()) {
            bot.session().send(new ServerboundSetCommandBlockPacket(
                    absoluteCorePosition(),
                    command,
                    kaboom ? CommandBlockMode.AUTO : CommandBlockMode.REDSTONE,
                    true,
                    false,
                    true
            ));

            incrementBlock();
        } else if (command.length() < 256) {
            bot.chat().send("/" + command);
        }
    }

    public CompletableFuture<CompoundTag> runTracked (String command) {
        final Vector3i position = absoluteCorePosition();

        run(command);

        if (!bot.options().useCore()) return null;

        final int transactionId = nextTransactionId++;

        // promises are renamed to future lmao
        final CompletableFuture<CompoundTag> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final Runnable afterTick = () -> bot.session().send(new ServerboundBlockEntityTagQuery(transactionId, position));

        bot.executor().schedule(afterTick, 50, TimeUnit.MILLISECONDS);

        return future;
    }

    public void runPlaceBlock (String command) {
        if (!ready || !bot.options().useCore()) return;

        final CompoundTag tag = new CompoundTag("");
        final CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
        blockEntityTag.put(new StringTag("Command", command));
        blockEntityTag.put(new ByteTag("auto", (byte) 1));
        blockEntityTag.put(new ByteTag("TrackOutput", (byte) 1));
        tag.put(blockEntityTag);

        final Vector3i temporaryBlockPosition = Vector3i.from(
                bot.position().position().getX(),
                bot.position().position().getY() - 1,
                bot.position().position().getZ()
        );

        final Session session = bot.session();
        session.send(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(kaboom ? 492 /* repeating command block id */ : 373 /* command block id */, 64, tag)));
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, 1));
    }

    public void packetReceived (ClientboundTagQueryPacket packet) {
        transactions.get(packet.getTransactionId()).complete(packet.getNbt());
    }

    public void packetReceived (ClientboundBlockUpdatePacket packet) {
        final BlockChangeEntry entry = packet.getEntry();

        if (isCommandBlock(entry.getBlock())) return;

        final Vector3i position = entry.getPosition();

        if (isCore(position)) refill();
    }

    public void packetReceived (ClientboundSectionBlocksUpdatePacket packet) {
        final BlockChangeEntry[] entries = packet.getEntries();

        boolean willRefill = false;

        for (BlockChangeEntry entry : entries) {
            final Vector3i position = entry.getPosition();

            if (isCommandBlock(entry.getBlock())) return;

            if (isCore(position)) willRefill = true;
        }

        if (willRefill) refill();
    }

    private boolean isCommandBlock (int blockState) {
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

    public void packetReceived (ClientboundLevelChunkWithLightPacket packet) {
        final Vector3i position = Vector3i.from(packet.getX() * 16, 0, packet.getZ() * 16); // mabe

        if (isCore(position)) refill();
    }

    public Vector3i absoluteCorePosition () {
        return relativeCorePosition.add(origin);
    }

    // ported from chomens bot js
    private boolean isCore (Vector3i position) {
        return
                position.getX() >= origin.getX() && position.getX() <= originEnd.getX() &&
                        position.getY() >= origin.getY() && position.getY() <= originEnd.getY() &&
                        position.getZ() >= origin.getZ() && position.getZ() <= originEnd.getZ();
    }

    private void incrementBlock () {
        int x = relativeCorePosition.getX();
        int y = relativeCorePosition.getY();
        int z = relativeCorePosition.getZ();

        x++;

        if (x > coreEnd.getX()) {
            x = coreStart.getX();
            z++;
        }

        if (z > coreEnd.getZ()) {
            z = coreStart.getZ();
            y++;
        }

        if (y > coreEnd.getY()) {
            x = coreStart.getX();
            y = coreStart.getY();
            z = coreStart.getZ();
        }

        relativeCorePosition = Vector3i.from(x, y, z);
    }

    @Override
    public void positionChange (Vector3i position) {
        // hm?
        if (
                coreStart.getX() == 0 &&
                        coreStart.getY() == 0 &&
                        coreStart.getZ() == 0 &&

                        coreEnd.getX() == 15 &&
                        coreEnd.getY() == 2 &&
                        coreEnd.getZ() == 15
        ) {
            origin = Vector3i.from(
                    Math.floor((double) bot.position().position().getX() / 16) * 16,
                    0,
                    Math.floor((double) bot.position().position().getZ() / 16) * 16
            );
        } else {
            origin = Vector3i.from(
                    bot.position().position().getX(),
                    0,
                    bot.position().position().getZ()
            );
        }
        originEnd = origin.add(coreEnd);

        refill();

        if (!ready) {
            ready = true;

            refillTask = bot.executor().scheduleAtFixedRate(this::refill, 0, bot.config().core().refillInterval(), TimeUnit.MILLISECONDS);
            for (Listener listener : listeners) listener.ready();
        }
    }

    public void reset () {
        relativeCorePosition = Vector3i.from(coreStart);
    }

    public void refill () {
        if (!ready) return;

        final String command = String.format(
                "minecraft:fill %s %s %s %s %s %s minecraft:command_block{CustomName:'%s'}",

                coreStart.getX() + origin.getX(),
                coreStart.getY(),
                coreStart.getZ() + origin.getZ(),

                coreEnd.getX() + origin.getX(),
                coreEnd.getY(),
                coreEnd.getZ() + origin.getZ(),

                bot.config().core().customName()
        );

//        bot.chat().send(command);

        runPlaceBlock(command);
    }

    public static class Listener {
        public void ready () {}
    }

    public void addListener (Listener listener) { listeners.add(listener); }
}
