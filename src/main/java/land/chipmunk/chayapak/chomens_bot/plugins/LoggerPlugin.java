package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundDisconnectPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.ConnectedEvent;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Logger;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;

public class LoggerPlugin extends ChatPlugin.ChatListener {
    private final Bot bot;

    private boolean addedListener = false;

    private boolean disconencted = false;

    public LoggerPlugin(Bot bot) {
        this.bot = bot;

        bot.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ClientboundDisconnectPacket) packetReceived((ClientboundDisconnectPacket) packet);
            }

            public void packetReceived (ClientboundDisconnectPacket packet) {
                disconencted = true;

                final String reason = ComponentUtilities.stringifyAnsi(packet.getReason());
                log("Disconnected from " + bot.host() + ":" + bot.port() + ", reason: " + reason);
            }

            @Override
            public void connected (ConnectedEvent event) {
                log("Successfully connected to: " + bot.host() + ":" + bot.port());

                if (addedListener) return;
                bot.chat().addListener(LoggerPlugin.this);
                addedListener = true;
            }

            @Override
            public void disconnected (DisconnectedEvent event) {
                if (disconencted) return;
                log("Disconnected from " + bot.host() + ":" + bot.port() + ", reason: " + event.getReason());
            }
        });
    }

    public void log (String message) {
        final String formattedMessage = String.format(
                "[%s] %s",
                bot.host() + ":" + bot.port(),
                message
        );
        bot.console().reader().printAbove(formattedMessage);
        Logger.log(
                formattedMessage.replaceAll( // use replaceAll for regexes, use replace for normal string
                        "\u001B\\[[;\\d]*[ -/]*[@-~]",
                        ""
                )
        );
    }

    @Override
    public void systemMessageReceived(String message, Component component) {
        final String ansiMessage = ComponentUtilities.stringifyAnsi(component);
        log(ansiMessage);
    }
}
