package land.chipmunk.chayapak.chomens_bot.plugins.testing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.Rotation;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.plugins.PositionPlugin;
import land.chipmunk.chayapak.chomens_bot.plugins.TickPlugin;
import org.cloudburstmc.math.vector.Vector3f;

import java.time.Instant;

public class OrbitPlugin extends PositionPlugin.Listener {
    private final Bot bot;

    public OrbitPlugin (Bot bot) {
        this.bot = bot;

        bot.position().addListener(this);

        bot.tick().addListener(new TickPlugin.Listener() {
            @Override
            public void onTick() {
                tick();
            }
        });
    }

    private Vector3f playerPos = null;
    private Rotation rotation = null;

    @Override
    public void playerMoved(MutablePlayerListEntry player, Vector3f position, Rotation rotation) {
        if (!player.profile().getName().equals("chayapak")) return;
        playerPos = position;
        this.rotation = rotation;
    }

    public void tick () {
        if (playerPos == null) return;

        final double now = Instant.now().toEpochMilli();

        final double x = playerPos.getX() + (Math.sin(((now / (1000 * 30) * 10)) * Math.PI * 2) * 5);
        final double y = playerPos.getY();
        final double z = playerPos.getZ() + (Math.cos(((now / (1000 * 30) * 10)) * Math.PI * 2) * 5);

        bot.session().send(
                new ServerboundMovePlayerPosRotPacket(
                        false,
                        x,
                        y,
                        z,
                        rotation.yaw,
                        rotation.pitch
                )
        );
    }
}
