package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import lombok.Getter;
import lombok.Setter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class PacketSnifferPlugin extends Bot.Listener {
    @Getter @Setter private boolean enabled = false;

    private OutputStreamWriter writer;

    public PacketSnifferPlugin (Bot bot) {
        if (!enabled) return;

        try {
            writer = new OutputStreamWriter(new FileOutputStream("packets.log"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }

        bot.addListener(this);
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        try {
            writer.write(packet.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
