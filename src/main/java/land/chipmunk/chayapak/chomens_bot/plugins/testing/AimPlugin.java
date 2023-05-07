package land.chipmunk.chayapak.chomens_bot.plugins.testing;

import com.github.steveice10.mc.protocol.packet.ingame.serverbound.player.ServerboundMovePlayerPosRotPacket;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.data.Rotation;
import land.chipmunk.chayapak.chomens_bot.plugins.PositionPlugin;
import org.cloudburstmc.math.vector.Vector3f;

// unused lol
public class AimPlugin extends PositionPlugin.Listener {
    private final Bot bot;

    public AimPlugin(Bot bot) {
        this.bot = bot;
        bot.position().addListener(this);
    }

    @Override
    public void playerMoved (MutablePlayerListEntry player, Vector3f position, Rotation rotation) {
        // final String name = player.profile().getName();

        // if (!name.equals("chayapak")) return;

        // Author: ChatGPT
        final double dx = bot.position().position().getX() - position.getX();
        final double dy = bot.position().position().getY() - position.getY();
        final double dz = bot.position().position().getZ() - position.getZ();
        final double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        final double pitchRadians = Math.asin(dy / distance);
        final double pitchDegrees = Math.toDegrees(pitchRadians);

        bot.session().send(
                new ServerboundMovePlayerPosRotPacket(
                        false,
                        bot.position().position().getX(),
                        bot.position().position().getY(),
                        bot.position().position().getZ(),
                        rotation.yaw - 180,
                        (float) pitchDegrees
                )
        );
    }
}
