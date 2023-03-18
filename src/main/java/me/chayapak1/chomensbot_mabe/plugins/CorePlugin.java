package me.chayapak1.chomensbot_mabe.plugins;

import com.github.steveice10.mc.protocol.data.game.level.block.CommandBlockMode;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.inventory.ServerboundSetCommandBlockPacket;
import com.nukkitx.math.vector.Vector3i;
import me.chayapak1.chomensbot_mabe.Bot;

public class CorePlugin extends PositionPlugin.PositionListener {
    private final Bot bot;

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
    }

    public void refill () {
        final String command = String.format(
                "/minecraft:fill %s %s %s %s %s %s minecraft:command_block",

                coreStart.getX() + origin.getX(),
                coreStart.getY(),
                coreStart.getZ() + origin.getZ(),

                coreEnd.getX() + origin.getX(),
                coreEnd.getY(),
                coreEnd.getZ() + origin.getZ()
        );
        bot.chat().send(command);
    }
}
