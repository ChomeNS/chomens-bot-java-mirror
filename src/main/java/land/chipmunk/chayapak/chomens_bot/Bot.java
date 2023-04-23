package land.chipmunk.chayapak.chomens_bot;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import land.chipmunk.chayapak.chomens_bot.plugins.*;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
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

    @Getter private final Configuration.BotOption options;

    @Getter private final Configuration config;

    @Getter private List<Bot> allBots;

    @Getter private String username;

    @Getter public Session session;

    @Getter private boolean loggedIn = false;

    @Getter private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(100);

    @Getter @Setter private ConsolePlugin console;
    @Getter @Setter private LoggerPlugin logger; // in ConsolePlugin
    @Getter @Setter private DiscordPlugin discord; // same for this one too

    @Getter private ChatPlugin chat;
    @Getter private PositionPlugin position;
    @Getter private SelfCarePlugin selfCare;
    @Getter private CorePlugin core;
    @Getter private PlayersPlugin players;
    @Getter private TabCompletePlugin tabComplete;
    @Getter private CommandHandlerPlugin commandHandler;
    @Getter private ChatCommandHandlerPlugin chatCommandHandler;
    @Getter private HashingPlugin hashing;
    @Getter private BossbarManagerPlugin bossbar;
    @Getter private MusicPlayerPlugin music;
    @Getter private TPSPlugin tps;
    @Getter private EvalRunnerPlugin eval;
    @Getter private ClearChatUsernamePlugin clearChatUsername;
    @Getter private TrustedPlugin trusted;
    @Getter private BruhifyPlugin bruhify;
    @Getter private GrepLogPlugin grepLog;
    @Getter private CloopPlugin cloop;
    @Getter private MazePlugin maze;
    @Getter private ExploitsPlugin exploits;

    public Bot (Configuration.BotOption botOption, List<Bot> allBots, Configuration config) {
        this.host = botOption.host;
        this.port = botOption.port;

        this.options = botOption;

        this.allBots = allBots;

        this.config = config;

        ConsolePlugin.addListener(new ConsolePlugin.Listener() {
            @Override
            public void ready() {
                Bot.this.ready();
            }
        });
    }

    public void ready () {
        this.chat = new ChatPlugin(this);
        this.position = new PositionPlugin(this);
        this.selfCare = new SelfCarePlugin(this);
        this.core = new CorePlugin(this);
        this.players = new PlayersPlugin(this);
        this.tabComplete = new TabCompletePlugin(this);
        this.commandHandler = new CommandHandlerPlugin(this);
        this.chatCommandHandler = new ChatCommandHandlerPlugin(this);
        this.hashing = new HashingPlugin(this);
        this.bossbar = new BossbarManagerPlugin(this);
        this.music = new MusicPlayerPlugin(this);
        this.tps = new TPSPlugin(this);
        this.eval = new EvalRunnerPlugin(this);
        this.clearChatUsername = new ClearChatUsernamePlugin(this);
        this.trusted = new TrustedPlugin(this);
        this.bruhify = new BruhifyPlugin(this);
        this.grepLog = new GrepLogPlugin(this);
        this.cloop = new CloopPlugin(this);
        this.maze = new MazePlugin(this);
        this.exploits = new ExploitsPlugin(this);

        reconnect();
    }

    public void reconnect () {
        final String _username = options.username();

        if (_username == null) username = RandomStringUtils.randomAlphabetic(8);
        else username = _username;

        Session session = new TcpClientSession(host, port, new MinecraftProtocol(username), null);

        this.session = session;

        session.addListener(new SessionAdapter() {
            // same stuff over and over yup

            @Override
            public void packetReceived(Session session, Packet packet) {
                for (SessionListener listener : listeners) {
                    listener.packetReceived(session, packet);
                }

                if (packet instanceof ClientboundLoginPacket) {
                    for (SessionListener listener : listeners) {
                        loggedIn = true;
                        listener.connected(new ConnectedEvent(session));
                    }
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
                loggedIn = false;

                int reconnectDelay = config.reconnectDelay();

                final String stringMessage = ComponentUtilities.stringify(disconnectedEvent.getReason());

                // this part is ported from chomens bot js
                if (
                        stringMessage.equals("Wait 5 seconds before connecting, thanks! :)") ||
                                stringMessage.equals("You are logging in too fast, try again later.")
                ) reconnectDelay = 1000 * 7;

                executor.schedule(() -> reconnect(), reconnectDelay, TimeUnit.MILLISECONDS);

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
