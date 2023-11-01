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
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
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
import net.kyori.adventure.text.BlockNBTComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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
    private final Map<Integer, CompletableFuture<Component>> transactions = new HashMap<>();

    private final List<Double> secrets = new ArrayList<>();

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
                if (packet instanceof ClientboundBlockUpdatePacket) CorePlugin.this.packetReceived((ClientboundBlockUpdatePacket) packet);
                else if (packet instanceof ClientboundSectionBlocksUpdatePacket) CorePlugin.this.packetReceived((ClientboundSectionBlocksUpdatePacket) packet);
                else if (packet instanceof ClientboundLevelChunkWithLightPacket) CorePlugin.this.packetReceived((ClientboundLevelChunkWithLightPacket) packet);
            }
        });

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public boolean systemMessageReceived(Component component, String string, String ansi) {
                return CorePlugin.this.systemMessageReceived(component);
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

    // thanks chipmunk for this new tellraw method :3
    public CompletableFuture<Component> runTracked (String command) {
        if (!bot.options.useCore) return null;

        final Vector3i coreBlock = block;

        run(command);

        final int transactionId = nextTransactionId++;

        final CompletableFuture<Component> future = new CompletableFuture<>();
        transactions.put(transactionId, future);

        final double secret = Math.random(); // it is uh bad whatever
        secrets.add(secret);

        bot.chat.tellraw(
                Component
                        .text(secret)
                        .append(Component.text(transactionId))
                        .append(
                                Component.blockNBT(
                                        "LastOutput",

                                        // is there a better way of doing this?
                                        BlockNBTComponent.Pos.fromString(
                                                coreBlock.getX() + " " +
                                                        coreBlock.getY() + " " +
                                                        coreBlock.getZ()
                                        )
                                )
                        ),
                bot.profile.getId()
        );

        return future;
    }

    private boolean systemMessageReceived (Component component) {
        if (!(component instanceof TextComponent textComponent)) return true;

        try {
            if (!secrets.contains(Double.parseDouble(textComponent.content()))) return true;

            final List<Component> children = component.children();

            if (children.size() > 2 || children.isEmpty()) return true;

            if (children.size() == 1) return false;

            final int transactionId = Integer.parseInt(((TextComponent) children.get(0)).content());

            if (!transactions.containsKey(transactionId)) return true;

            final CompletableFuture<Component> future = transactions.get(transactionId);

            final String stringLastOutput = ((TextComponent) children.get(1)).content();

            future.complete(Component.join(JoinConfiguration.separator(Component.space()), GsonComponentSerializer.gson().deserialize(stringLastOutput).children()));

            return false;
        } catch (ClassCastException | NumberFormatException ignored) {}

        return true;
    }

    public void runPlaceBlock (String command) {
        if (!ready || !bot.options.useCore) return;

        final CompoundTag tag = new CompoundTag("");
        final CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
        blockEntityTag.put(new StringTag("Command", command));
        blockEntityTag.put(new ByteTag("auto", (byte) 1));
        blockEntityTag.put(new ByteTag("TrackOutput", (byte) 1));
        blockEntityTag.put(new StringTag("CustomName", bot.config.core.customName));
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

    private boolean isCommandBlockUpdate (int blockState) {
        return
                // command block
                (
                        blockState >= 7902 &&
                                blockState <= 7913
                ) ||

                        // chain command block
                        (
                                blockState == 12533
//                                blockState >= 12371 &&
//                                        blockState <= 12382
                        ) ||

                        // repeating command block
                        (
                                blockState == 12521
//                                blockState >= 12359 &&
//                                        blockState <= 12370
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
                (int) (fromSize.getX() + Math.floor((double) bot.position.position.getX() / 16) * 16),
                MathUtilities.clamp(fromSize.getY(), bot.world.minY, bot.world.maxY),
                (int) (fromSize.getZ() + Math.floor((double) bot.position.position.getZ() / 16) * 16)
        );

        to = Vector3i.from(
                (int) (toSize.getX() + Math.floor((double) bot.position.position.getX() / 16) * 16),
                MathUtilities.clamp(toSize.getY(), bot.world.minY, bot.world.maxY),
                (int) (toSize.getZ() + Math.floor((double) bot.position.position.getZ() / 16) * 16)
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
