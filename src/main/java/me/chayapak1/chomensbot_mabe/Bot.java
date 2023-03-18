package me.chayapak1.chomensbot_mabe;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import lombok.Getter;
import me.chayapak1.chomensbot_mabe.plugins.*;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bot {
    private final ArrayList<SessionListener>listeners = new ArrayList<>();

    @Getter private final String host;
    @Getter private final int port;
    @Getter private final String username;

    @Getter private final Session session;

    @Getter private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    @Getter private final ChatPlugin chat = new ChatPlugin(this);
    @Getter private final LoggerPlugin logger = new LoggerPlugin(this);
    @Getter private final SelfCarePlugin selfCare = new SelfCarePlugin(this);
    @Getter private final ConsolePlugin console = new ConsolePlugin(this);
    @Getter private final PositionPlugin position = new PositionPlugin(this);
    @Getter private final CorePlugin core = new CorePlugin(this);
    @Getter private final CommandHandlerPlugin commandHandler = new CommandHandlerPlugin();
    @Getter private final ChatCommandHandlerPlugin chatCommandHandler = new ChatCommandHandlerPlugin(this);

    public Bot (String host, int port, String username) {
        this.host = host;
        this.port = port;
        this.username = username;
        Session session = new TcpClientSession(host, port, new MinecraftProtocol(username), null);
        this.session = session;

        session.addListener(new SessionAdapter() {
            // same stuff over and over yup

            @Override
            public void packetReceived(Session session, Packet packet) {
                  for (SessionListener listener : listeners) {
                      if (packet instanceof ClientboundLoginPacket) {
                          listener.connected(new ConnectedEvent(session));
                      }
                      listener.packetReceived(session, packet);
                  }
            }

            @Override
            public void packetSending(PacketSendingEvent packetSendingEvent) {
                for (SessionListener listener : listeners) {
                    listener.packetSending(packetSendingEvent);
                }
            }

            @Override
            public void packetSent(Session session, Packet packet) {
                for (SessionListener listener : listeners) {
                    listener.packetSent(session, packet);
                }
            }

            @Override
            public void packetError(PacketErrorEvent packetErrorEvent) {
                for (SessionListener listener : listeners) {
                    listener.packetError(packetErrorEvent);
                }
            }

            @Override
            public void disconnecting(DisconnectingEvent disconnectingEvent) {
                for (SessionListener listener : listeners) {
                    listener.disconnecting(disconnectingEvent);
                }
            }

            @Override
            public void disconnected(DisconnectedEvent disconnectedEvent) {
                for (SessionListener listener : listeners) {
                    listener.disconnected(disconnectedEvent);
                }
            }
        });

        session.connect();
    }

    public void addListener (SessionListener listener) {
        listeners.add(listener);
    }
}
