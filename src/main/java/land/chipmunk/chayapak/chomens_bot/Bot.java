package land.chipmunk.chayapak.chomens_bot;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import land.chipmunk.chayapak.chomens_bot.plugins.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot {
    private final ArrayList<SessionListener> listeners = new ArrayList<>();

    @Getter private final String host;
    @Getter private final int port;
    private final String _username;
    @Getter private final boolean kaboom;
    @Getter private final String serverName;
    @Getter private final List<Bot> allBots;
    @Getter private final Configuration config;

    @Getter private String username;

    @Getter private Session session;

    @Getter private ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);

    @Getter @Setter private ConsolePlugin console;
    @Getter @Setter private LoggerPlugin logger; // in ConsolePlugin
    @Getter @Setter private DiscordPlugin discord;
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
    @Getter private final ClearChatUsernamePlugin clearChatUsername;
    @Getter private final TrustedPlugin trusted;
    @Getter private final BruhifyPlugin bruhify;
    @Getter private final GrepLogPlugin grepLog;

    public Bot (String host, int port, String _username, boolean kaboom, String serverName, List<Bot> allBots, Configuration config) {
        this.host = host;
        this.port = port;
        this._username = _username;
        this.kaboom = kaboom;
        this.serverName = serverName;
        this.allBots = allBots;
        this.config = config;

        try {
            DiscordPlugin.readyLatch().await();
        } catch (InterruptedException ignored) { System.exit(1); }

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
        this.clearChatUsername = new ClearChatUsernamePlugin(this);
        this.trusted = new TrustedPlugin(this);
        this.bruhify = new BruhifyPlugin(this);
        this.grepLog = new GrepLogPlugin(this);

        try {
            Thread.sleep(1000); // real
        } catch (InterruptedException ignored) {
            System.exit(1);
        }

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
                if (packet instanceof ClientboundLoginPacket) {
                    for (SessionListener listener : listeners) {
                        listener.connected(new ConnectedEvent(session));
                    }
                }

                for (SessionListener listener : listeners) {
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
                packetErrorEvent.setSuppress(true); // fix the ohio sus exploit
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

                executor.schedule(() -> reconnect(), reconnectDelay, TimeUnit.MILLISECONDS);
            }
        });

        session.connect();
    }

    public void addListener (SessionListener listener) {
        listeners.add(listener);
    }
}
