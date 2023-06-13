package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundCustomPayloadPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundCustomPayloadPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.util.Arrays;

// exists for some reason, still wip and will be finished in the next 69 years
// and i prob implemented it in a wrong way lol
// at least when you do `/voicechat test (bot username)` it will show `Client not connected`
// instead of `(bot username) does not have Simple Voice Chat installed`
public class VoiceChatPlugin extends Bot.Listener {
    private final Bot bot;

    public VoiceChatPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
        else if (packet instanceof ClientboundCustomPayloadPacket) packetReceived((ClientboundCustomPayloadPacket) packet);
    }

    public void packetReceived(ClientboundLoginPacket ignored) {
        // totally didn't use a real minecraft client with voicechat mod to get this
        bot.session().send(new ServerboundCustomPayloadPacket(
                "minecraft:brand",
                "\u0006fabric".getBytes() // should i use fabric here?
        ));

        bot.session().send(new ServerboundCustomPayloadPacket(
                "voicechat:request_secret",
                new byte[] { 0, 0, 0, 17 } // what are these bytes?
        ));
    }

    public void packetReceived(ClientboundCustomPayloadPacket packet) {
        // sus
        /*
        System.out.println(packet.getChannel());
        System.out.println(Arrays.toString(packet.getData()));
        System.out.println(new String(packet.getData()));
         */
    }
}
