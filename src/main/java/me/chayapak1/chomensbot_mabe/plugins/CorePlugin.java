package me.chayapak1.chomensbot_mabe.plugins;

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
import com.github.steveice10.packetlib.Session;
import com.nukkitx.math.vector.Vector3i;
import lombok.Getter;
import me.chayapak1.chomensbot_mabe.Bot;

import java.util.ArrayList;
import java.util.List;

public class CorePlugin extends PositionPlugin.PositionListener {
    private final Bot bot;

    @Getter private final List<Listener> listeners = new ArrayList<>();

    @Getter private boolean ready = false;

    public final Vector3i coreStart = Vector3i.from(0, 0, 0);
    public final Vector3i coreEnd = Vector3i.from(15, 2, 15);

    public Vector3i origin;

    public Vector3i relativeCorePosition = Vector3i.from(coreStart);

    public CorePlugin (Bot bot) {
        this.bot = bot;

        bot.position().addListener(this);
    }

    public void run (String command) {
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
                CommandBlockMode.REDSTONE,
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

        if (x >= coreEnd.getX()) {
            x = coreStart.getX();
            z++;
        }

        if (z >= coreEnd.getZ()) {
            z = coreStart.getZ();
            y++;
        }

        if (y >= coreEnd.getY()) {
            x = coreStart.getX();
            y = coreStart.getY();
            z = coreStart.getZ();
        }

        relativeCorePosition = Vector3i.from(x, y, z);
    }

    @Override
    public void positionChange (Vector3i position) {
        origin = Vector3i.from(
                bot.position().position().getX(),
                0,
                bot.position().position().getZ()
        );
        refill();

        if (!ready) {
            ready = true;
            for (Listener listener : listeners) listener.ready();
        }
    }

    public void refill () {
        final String command = String.format(
                "/minecraft:fill %s %s %s %s %s %s minecraft:command_block{CustomName:'%s'}",

                coreStart.getX() + origin.getX(),
                coreStart.getY(),
                coreStart.getZ() + origin.getZ(),

                coreEnd.getX() + origin.getX(),
                coreEnd.getY(),
                coreEnd.getZ() + origin.getZ(),

                bot.config().core().customName()
        );

        bot.chat().send(command);

        // no work :(
//        CompoundTag tag = new CompoundTag("tag");
//        CompoundTag blockEntityTag = new CompoundTag("BlockEntityTag");
//        blockEntityTag.getValue().put("Command", new StringTag("Command", command));
//        blockEntityTag.getValue().put("auto", new ByteTag("auto", (byte) 1));
//        blockEntityTag.getValue().put("TrackOutput", new ByteTag("TrackOutput", (byte) 1));
//        tag.getValue().put("BlockEntityTag", blockEntityTag);
//
//        final Vector3i temporaryBlockPosition = Vector3i.from(
//                bot.position().position().getX(),
//                bot.position().position().getX() - 1,
//                bot.position().position().getZ()
//        );
//
//        final Session session = bot.session();
//        session.send(new ServerboundSetCreativeModeSlotPacket(45, new ItemStack(347 /* command block id */, 64, tag)));
//        session.send(new ServerboundPlayerActionPacket(PlayerAction.START_DIGGING, temporaryBlockPosition, Direction.NORTH, 0));
//        session.send(new ServerboundUseItemOnPacket(temporaryBlockPosition, Direction.NORTH, Hand.OFF_HAND, 0.5f, 0.5f, 0.5f, false, 0));
    }

    public static class Listener {
        public void ready () {}
    }

    public void addListener (Listener listener) { listeners.add(listener); }
}
