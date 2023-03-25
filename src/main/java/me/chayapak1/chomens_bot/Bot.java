package me.chayapak1.chomens_bot;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import lombok.Getter;
import lombok.Setter;
import me.chayapak1.chomens_bot.plugins.*;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Bot {
    private final ArrayList<SessionListener> listeners = new ArrayList<>();

    @Getter private final String host;
    @Getter private final int port;
    private final String _username;
    @Getter private final boolean kaboom;
    @Getter private final List<Bot> allBots;
    @Getter private final Configuration config;

    @Getter private String username;

    @Getter private Session session;

    @Getter @Setter private ConsolePlugin console;
    @Getter @Setter private LoggerPlugin logger; // in ConsolePlugin
    @Getter private final ChatPlugin chat;
    @Getter private final SelfCarePlugin selfCare;
    @Getter private final PositionPlugin position;
    @Getter private final CorePlugin core;
    @Getter private final PlayersPlugin players;
    @Getter private final TabCompletePlugin tabComplete;
    @Getter private final CommandHandlerPlugin commandHandler;
    @Getter private final ChatCommandHandlerPlugin chatCommandHandler;
    @Getter private final HashingPlugin hashing;
    @Getter private final MusicPlayerPlugin music;
    @Getter private final TPSPlugin tps;
    @Getter private final EvalRunnerPlugin eval;

    public Bot (String host, int port, String _username, boolean kaboom, List<Bot> allBots, Configuration config) {
        this.host = host;
        this.port = port;
        this._username = _username;
        this.kaboom = kaboom;
        this.allBots = allBots;
        this.config = config;

        this.chat = new ChatPlugin(this);
        this.selfCare = new SelfCarePlugin(this);
        this.position = new PositionPlugin(this);
        this.core = new CorePlugin(this);
        this.players = new PlayersPlugin(this);
        this.tabComplete = new TabCompletePlugin(this);
        this.commandHandler = new CommandHandlerPlugin();
        this.chatCommandHandler = new ChatCommandHandlerPlugin(this);
        this.hashing = new HashingPlugin(this);
        this.music = new MusicPlayerPlugin(this);
        this.tps = new TPSPlugin(this);
        this.eval = new EvalRunnerPlugin(this);

        reconnect();
    }

    public void reconnect () {
        if (_username == null) username = RandomStringUtils.randomAlphabetic(8);
        else username = _username;

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
                packetErrorEvent.setSuppress(false); // ? idk what this does but whatever
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

                final int reconnectDelay = config.reconnectDelay();

                if (reconnectDelay < 0) return; // to disable reconnecting

                final Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        reconnect();
                    }
                }, reconnectDelay);
            }
        });

        session.connect();
    }

    public void addListener (SessionListener listener) {
        listeners.add(listener);
    }
}
