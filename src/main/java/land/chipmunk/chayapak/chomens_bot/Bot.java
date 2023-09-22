package land.chipmunk.chayapak.chomens_bot;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.player.HandPreference;
import com.github.steveice10.mc.protocol.data.game.setting.ChatVisibility;
import com.github.steveice10.mc.protocol.data.game.setting.SkinPart;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientInformationPacket;
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import land.chipmunk.chayapak.chomens_bot.plugins.*;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.RandomStringUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot {
    private final ArrayList<Listener> listeners = new ArrayList<>();

    public final String host;
    public final int port;

    public final Configuration.BotOption options;

    public final Configuration config;

    public final List<Bot> bots;

    public String username;

    public GameProfile profile;

    public Session session;

    public boolean loggedIn = false;

    public final ExecutorService executorService = Main.executorService;
    public final ScheduledExecutorService executor = Main.executor;

    public ConsolePlugin console;
    public LoggerPlugin logger; // in ConsolePlugin
    public DiscordPlugin discord; // same for this one too

    public TickPlugin tick;
    public ChatPlugin chat;
    public CommandSpyPlugin commandSpy;
    public PositionPlugin position;
    public ServerPluginsManagerPlugin serverPluginsManager;
    public SelfCarePlugin selfCare;
    public CorePlugin core;
    public TeamPlugin team;
    public PlayersPlugin players;
    public TabCompletePlugin tabComplete;
    public CommandHandlerPlugin commandHandler;
    public ChatCommandHandlerPlugin chatCommandHandler;
    public HashingPlugin hashing;
    public BossbarManagerPlugin bossbar;
    public MusicPlayerPlugin music;
    public TPSPlugin tps;
    public TrustedPlugin trusted;
    public BruhifyPlugin bruhify;
    public CloopPlugin cloop;
    public ExploitsPlugin exploits;
    public FilterPlugin filter;
    public CommandSuggestionPlugin commandSuggestion;
    public MailPlugin mail;
    public PacketSnifferPlugin packetSniffer;
    public VoiceChatPlugin voiceChat;
    public TagPlugin tag;
    public WorldPlugin world;
    public AuthPlugin auth;
    public ScreensharePlugin screenshare;
    public FormatCheckerPlugin formatChecker;

    public Bot (Configuration.BotOption botOption, List<Bot> bots, Configuration config) {
        this.host = botOption.host;
        this.port = botOption.port;

        this.options = botOption;

        this.bots = bots;

        this.config = config;

        ConsolePlugin.addListener(new ConsolePlugin.Listener() {
            @Override
            public void ready() {
                Bot.this.ready();
            }
        });
    }

    public void ready () {
        this.tick = new TickPlugin(this);
        this.chat = new ChatPlugin(this);
        this.commandSpy = new CommandSpyPlugin(this);
        this.position = new PositionPlugin(this);
        this.serverPluginsManager = new ServerPluginsManagerPlugin(this);
        this.selfCare = new SelfCarePlugin(this);
        this.core = new CorePlugin(this);
        this.team = new TeamPlugin(this);
        this.players = new PlayersPlugin(this);
        this.tabComplete = new TabCompletePlugin(this);
        this.commandHandler = new CommandHandlerPlugin(this);
        this.chatCommandHandler = new ChatCommandHandlerPlugin(this);
        this.hashing = new HashingPlugin(this);
        this.bossbar = new BossbarManagerPlugin(this);
        this.music = new MusicPlayerPlugin(this);
        this.tps = new TPSPlugin(this);
        this.trusted = new TrustedPlugin(this);
        this.bruhify = new BruhifyPlugin(this);
        this.cloop = new CloopPlugin(this);
        this.exploits = new ExploitsPlugin(this);
        this.filter = new FilterPlugin(this);
        this.commandSuggestion = new CommandSuggestionPlugin(this);
        this.mail = new MailPlugin(this);
        this.packetSniffer = new PacketSnifferPlugin(this);
        this.voiceChat = new VoiceChatPlugin(this);
        this.tag = new TagPlugin(this);
        this.world = new WorldPlugin(this);
        this.auth = new AuthPlugin(this);
//        this.screenshare = new ScreensharePlugin(this);
        this.formatChecker = new FormatCheckerPlugin(this);

        for (Listener listener : listeners) listener.loadedPlugins();

        reconnect();
    }

    private void reconnect () {
        if (session != null) session = null; // does this do nothing?

        for (Listener listener : listeners) {
            listener.connecting();
        }

        final String _username = options.username;

        if (_username == null) username = RandomStringUtilities.generate(8);
        else username = _username;

        Session session = new TcpClientSession(host, port, new MinecraftProtocol(username), null);

        this.session = session;

        session.addListener(new SessionAdapter() {
            // fard

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

                    final List<SkinPart> skinParts = new ArrayList<>();
                    skinParts.add(SkinPart.CAPE);
                    skinParts.add(SkinPart.JACKET);
                    skinParts.add(SkinPart.LEFT_SLEEVE);
                    skinParts.add(SkinPart.RIGHT_SLEEVE);
                    skinParts.add(SkinPart.LEFT_PANTS_LEG);
                    skinParts.add(SkinPart.RIGHT_PANTS_LEG);
                    skinParts.add(SkinPart.HAT);

                    session.send(
                            new ServerboundClientInformationPacket(
                                    ComponentUtilities.language.getOrDefault("language.code", "en-us"),
                                    16,
                                    ChatVisibility.FULL,
                                    true,
                                    skinParts,
                                    HandPreference.RIGHT_HAND,
                                    false,
                                    true
                            )
                    );
                } else if (packet instanceof ClientboundGameProfilePacket) packetReceived((ClientboundGameProfilePacket) packet);
            }

            public void packetReceived(ClientboundGameProfilePacket packet) {
                profile = packet.getProfile();
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

                final Throwable cause = disconnectedEvent.getCause();

                if (cause != null) {
                    // lazy fix (#69420)
                    if (cause instanceof OutOfMemoryError) System.exit(1);
                }

                int reconnectDelay = options.reconnectDelay;

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

        session.connect(false);
    }

    public void addListener (Listener listener) {
        listeners.add(listener);
    }

    public static class Listener extends SessionAdapter {
        public void connecting () {}
        public void loadedPlugins () {}
    }
}
