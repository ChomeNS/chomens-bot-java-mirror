package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.PacketSendingEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

// normally unused in the main instance of the bot
public class PacketSnifferPlugin extends Bot.Listener {
    private final Bot bot;

    public boolean enabled = false;

    private BufferedWriter writer;

    @SuppressWarnings("ConstantValue") // this can be set through servereval
    public PacketSnifferPlugin (Bot bot) {
        this.bot = bot;

        if (enabled) enable();

        bot.addListener(this);
    }

    public void enable () {
        enabled = true;

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
            bot.logger.error(e);
        }
    }

    @SuppressWarnings("unused")
    public void disable () {
        enabled = false;

        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            bot.logger.error(e);
        }
    }

    @Override
    public void packetReceived(Session session, Packet packet) {
        if (!enabled) return;

        try {
            writer.write(packet.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            bot.logger.error(e);
        }
    }

    @Override
    public void packetSending(PacketSendingEvent event) {
        if (!enabled) return;

        try {
            writer.write(event.getPacket().toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            bot.logger.error(e);
        }
    }
}
