package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.game.entity.object.Direction;
import com.github.steveice10.mc.protocol.data.game.entity.player.Hand;
import com.github.steveice10.mc.protocol.data.game.entity.player.PlayerAction;
import com.github.steveice10.mc.protocol.data.game.level.block.CommandBlockMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCreativeModeSlotPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundUseItemOnPacket;
import com.github.steveice10.opennbt.tag.builtin.ByteTag;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CorePlugin extends PositionPlugin.PositionListener {
    private final Bot bot;

    @Getter private final List<Listener> listeners = new ArrayList<>();

    @Getter private boolean ready = false;

    public final Vector3i coreStart = Vector3i.from(0, 0, 0);
    public Vector3i coreEnd;

    public Vector3i origin;

    public Vector3i relativeCorePosition = Vector3i.from(coreStart);

    private final boolean kaboom;

    public CorePlugin (Bot bot) {
        this.bot = bot;
        this.kaboom = bot.kaboom();

        bot.position().addListener(this);

        bot.addListener(new SessionAdapter() {
            @Override
            public void disconnected (DisconnectedEvent event) {
                ready = false;
            }
        });
    }

    public void run (String command) {
        if (!ready) return;

        bot.session().send(new ServerboundSetCommandBlockPacket(
                absoluteCorePosition(),
                "",
                CommandBlockMode.REDSTONE,
                false,
                false,
                false
        ));
        bot.session().send(new ServerboundSetCommandBlockPacket(
                absoluteCorePosition(),
                command,
                kaboom ? CommandBlockMode.AUTO : CommandBlockMode.REDSTONE,
                true,
                false,
                true
        ));

        incrementBlock();
    }

    public Vector3i absoluteCorePosition () {
        return relativeCorePosition.add(origin);
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
        coreEnd = Vector3i.from(15, bot.config().core().layers() - 1, 15);
        origin = Vector3i.from(
                bot.position().position().getX(),
                0,
                bot.position().position().getZ()
        );
        refill();

        if (!ready) {
            ready = true;

            bot.executor().scheduleAtFixedRate(this::refill, 0, bot.config().core().refillInterval(), TimeUnit.MILLISECONDS);
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

        Map<String, Tag> tag = new HashMap<>();
        Map<String, Tag> blockEntityTag = new HashMap<>();
        blockEntityTag.put("Command", new StringTag("Command", command));
        blockEntityTag.put("auto", new ByteTag("auto", (byte) 1));
        blockEntityTag.put("TrackOutput", new ByteTag("TrackOutput", (byte) 1));
        tag.put("BlockEntityTag", new CompoundTag("BlockEntityTag", blockEntityTag));

        final Vector3i temporaryBlockPosition = Vector3i.from(
                bot.position().position().getX(),
                bot.position().position().getY() - 1,
                bot.position().position().getZ()
        );

        final Session session = bot.session();
        session.send(new ServerboundSetCreativeModeSlotPacket(36, new ItemStack(kaboom ? 466 /* repeating command block id */ : 347 /* command block id */, 64, new CompoundTag("", tag))));
        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.UP, Hand.MAIN_HAND, 0.5f, 0.5f, 0.5f, false, 1));
    }

    public static class Listener {
        public void ready () {}
    }

    public void addListener (Listener listener) { listeners.add(listener); }
}
