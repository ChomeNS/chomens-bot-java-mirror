package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// normally unused in the main instance of the bot
public class PacketSnifferPlugin extends Bot.Listener {
    public final boolean enabled = false;

    private BufferedWriter writer;

    public PacketSnifferPlugin (Bot bot) {
        if (!enabled) return;

        final String name = String.format(
                "packets-%s-%s.log",
                bot.options.host,
                bot.options.port
        );

        final Path path = Path.of(name);

        try {
            if (!Files.exists(path)) Files.createFile(path);

            writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND);
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
