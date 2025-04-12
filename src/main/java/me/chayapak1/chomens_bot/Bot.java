package me.chayapak1.chomens_bot;

import me.chayapak1.chomens_bot.data.color.ColorPalette;
import me.chayapak1.chomens_bot.plugins.*;
import me.chayapak1.chomens_bot.util.ComponentUtilities;
import me.chayapak1.chomens_bot.util.RandomStringUtilities;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.network.BuiltinFlags;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.*;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.HandPreference;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ChatVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.setting.ParticleStatus;
import org.geysermc.mcprotocollib.protocol.data.game.setting.SkinPart;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundStoreCookiePacket;
import org.geysermc.mcprotocollib.protocol.packet.common.clientbound.ClientboundTransferPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundClientInformationPacket;
import org.geysermc.mcprotocollib.protocol.packet.common.serverbound.ServerboundCustomPayloadPacket;
import org.geysermc.mcprotocollib.protocol.packet.configuration.clientbound.ClientboundFinishConfigurationPacket;
import org.geysermc.mcprotocollib.protocol.packet.cookie.clientbound.ClientboundCookieRequestPacket;
import org.geysermc.mcprotocollib.protocol.packet.cookie.serverbound.ServerboundCookieResponsePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundPlayerLoadedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundCustomQueryPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginCompressionPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundLoginFinishedPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundCustomQueryAnswerPacket;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Bot extends SessionAdapter {
    private final List<Listener> listeners = new ArrayList<>();

    public final String host;
    public final int port;

    public final Configuration.BotOption options;

    public final Configuration config;
    public final ColorPalette colorPalette;

    public final List<Bot> bots;

    public String username;

    public GameProfile profile;

    public ClientSession session;

    private final Map<Key, byte[]> cookies = new HashMap<>();

    private boolean isTransferring = false;

    public boolean printDisconnectedCause = false;

    public int connectingTimes = 0;
    public int disconnectedTimes = 0;

    public boolean loggedIn = false;
    public long loginTime;

    public final ExecutorService executorService = Main.EXECUTOR_SERVICE;
    public final ScheduledExecutorService executor = Main.EXECUTOR;

    public final LoggerPlugin logger;
    public final TickPlugin tick;
    public final ChatPlugin chat;
    public final CommandSpyPlugin commandSpy;
    public final PositionPlugin position;
    public final ServerFeaturesPlugin serverFeatures;
    public final SelfCarePlugin selfCare;
    public final QueryPlugin query;
    public final ExtrasMessengerPlugin extrasMessenger;
    public final WorldPlugin world;
    public final CorePlugin core;
    public final TeamPlugin team;
    public final PlayersPlugin players;
    public final TabCompletePlugin tabComplete;
    public final CommandHandlerPlugin commandHandler;
    public final ChatCommandHandlerPlugin chatCommandHandler;
    public final HashingPlugin hashing;
    public final BossbarManagerPlugin bossbar;
    public final MusicPlayerPlugin music;
    public final TPSPlugin tps;
    public final EvalPlugin eval;
    public final TrustedPlugin trusted;
    public final GrepLogPlugin grepLog;
    public final BruhifyPlugin bruhify;
    public final CloopPlugin cloop;
    public final ExploitsPlugin exploits;
    public final FilterManagerPlugin filterManager;
    public final PlayerFilterPlugin playerFilter;
    public final CommandSuggestionPlugin commandSuggestion;
    public final MailPlugin mail;
    public final PacketSnifferPlugin packetSniffer;
    public final VoiceChatPlugin voiceChat;
    public final TeamJoinerPlugin teamJoiner;
    public final ChomeNSModIntegrationPlugin chomeNSMod;
    public final AuthPlugin auth;
    public final ScreensharePlugin screenshare = null;
    public final ClearChatNameAnnouncerPlugin clearChatNameAnnouncer;
    public final WhitelistPlugin whitelist;
    public final PlayersDatabasePlugin playersDatabase;
    public final IPFilterPlugin ipFilter;

    public Bot (
            final Configuration.BotOption botOption,
            final List<Bot> bots,
            final Configuration config
    ) {
        this.host = botOption.host;
        this.port = botOption.port;

        this.options = botOption;

        this.bots = bots;

        this.config = config;
        this.colorPalette = new ColorPalette(config.colorPalette);

        this.logger = new LoggerPlugin(this);
        this.tick = new TickPlugin(this);
        this.chat = new ChatPlugin(this);
        this.commandSpy = new CommandSpyPlugin(this);
        this.position = new PositionPlugin(this);
        this.serverFeatures = new ServerFeaturesPlugin(this);
        this.selfCare = new SelfCarePlugin(this);
        this.query = new QueryPlugin(this);
        this.extrasMessenger = new ExtrasMessengerPlugin(this);
        this.world = new WorldPlugin(this);
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
        this.eval = new EvalPlugin(this);
        this.trusted = new TrustedPlugin(this);
        this.grepLog = new GrepLogPlugin(this);
        this.bruhify = new BruhifyPlugin(this);
        this.cloop = new CloopPlugin(this);
        this.exploits = new ExploitsPlugin(this);
        this.filterManager = new FilterManagerPlugin(this);
        this.playerFilter = new PlayerFilterPlugin(this);
        this.commandSuggestion = new CommandSuggestionPlugin(this);
        this.mail = new MailPlugin(this);
        this.packetSniffer = new PacketSnifferPlugin(this);
        this.voiceChat = new VoiceChatPlugin(this);
        this.teamJoiner = new TeamJoinerPlugin(this);
        this.chomeNSMod = new ChomeNSModIntegrationPlugin(this);
        this.auth = new AuthPlugin(this);
        // this.screenshare = new ScreensharePlugin(this);
        this.clearChatNameAnnouncer = new ClearChatNameAnnouncerPlugin(this);
        this.whitelist = new WhitelistPlugin(this);
        this.playersDatabase = new PlayersDatabasePlugin(this);
        this.ipFilter = new IPFilterPlugin(this);
    }

    protected void connect () {
        for (final Listener listener : listeners) listener.loadedPlugins(this);

        reconnect();
    }

    private void reconnect () {
        if (session != null) session = null; // does this do nothing?

        connectingTimes++;

        for (final Listener listener : listeners) {
            listener.connecting();
        }

        if (!isTransferring) {
            username = options.username == null ?
                    RandomStringUtilities.generate(8) :
                    options.username;
        }

        final ClientSession session = ClientNetworkSessionFactory.factory()
                .setAddress(host, port)
                .setProtocol(new MinecraftProtocol(username))
                .create();

        this.session = session;

        // this is still needed since MinecraftProtocol will use this flag to set the
        // handshake intention to transfer, it will be set back to false at
        // login success
        session.setFlag(BuiltinFlags.CLIENT_TRANSFERRING, isTransferring);

        session.setFlag(MinecraftConstants.FOLLOW_TRANSFERS, false); // we have our own transfer handler

        session.setFlag(BuiltinFlags.ATTEMPT_SRV_RESOLVE, options.resolveSRV);

        session.addListener(this);

        session.connect(false);
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        for (final SessionListener listener : listeners) {
            try {
                listener.packetReceived(session, packet);
            } catch (final Exception e) {
                logger.error(e);
            }
        }

        if (packet instanceof final ClientboundLoginPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundFinishConfigurationPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundLoginFinishedPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundCustomQueryPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundCookieRequestPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundTransferPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundStoreCookiePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundLoginCompressionPacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundCustomPayloadPacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundLoginFinishedPacket packet) {
        profile = packet.getProfile();

        session.setFlag(BuiltinFlags.CLIENT_TRANSFERRING, false);
    }

    private void packetReceived (final ClientboundLoginPacket ignoredPacket) {
        loggedIn = true;
        loginTime = System.currentTimeMillis();
        connectingTimes = 0;
        disconnectedTimes = 0;

        for (final SessionListener listener : listeners) {
            listener.connected(new ConnectedEvent(session));
        }

        session.send(ServerboundPlayerLoadedPacket.INSTANCE);
    }

    private void packetReceived (final ClientboundCustomQueryPacket packet) {
        session.send(new ServerboundCustomQueryAnswerPacket(packet.getMessageId(), null));
    }

    private void packetReceived (final ClientboundCustomPayloadPacket packet) {
        if (!packet.getChannel().asString().equals("minecraft:register")) return;

        session.send(
                new ServerboundCustomPayloadPacket(
                        Key.key("minecraft", "register"),
                        "\0".getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private void packetReceived (final ClientboundCookieRequestPacket packet) {
        session.send(
                new ServerboundCookieResponsePacket(
                        packet.getKey(),
                        cookies.get(packet.getKey())
                )
        );
    }

    private void packetReceived (final ClientboundStoreCookiePacket packet) {
        cookies.put(packet.getKey(), packet.getPayload());
    }

    private void packetReceived (final ClientboundTransferPacket ignoredPacket) {
        this.isTransferring = true;

        session.disconnect(Component.translatable("disconnect.transfer"));
    }

    // we're not meant to send client information at finish configuration,
    // but if it works it works™
    private void packetReceived (final ClientboundFinishConfigurationPacket ignoredPacket) {
        // for voicechat
        session.send(new ServerboundCustomPayloadPacket(
                Key.key("minecraft:brand"),
                "\u0006fabric".getBytes() // should i use fabric here?
        ));

        // this enables all the skin parts (by default they are ALL DISABLED
        // which is why most bots when they use someone's skin they are just
        // kinda broken)
        final List<SkinPart> skinParts = new ArrayList<>(Arrays.asList(SkinPart.VALUES));

        // we also set other stuffs here
        session.send(
                new ServerboundClientInformationPacket(
                        ComponentUtilities.LANGUAGE.getOrDefault("language.code", "en_us"),
                        16,
                        ChatVisibility.FULL,
                        true,
                        skinParts,
                        HandPreference.RIGHT_HAND,
                        false,
                        false,
                        ParticleStatus.ALL
                )
        );
    }

    // MCProtocolLib devs forgot
    // "Negative values will disable compression, meaning the packet format should remain in the uncompressed packet format."
    // https://minecraft.wiki/w/Java_Edition_protocol#Set_Compression

    // they actually implemented this, but at this commit:
    // https://github.com/GeyserMC/MCProtocolLib/commit/f8460356db2b92fbf7cb506757fe8f87a011a1f7#diff-a9066adbcb6d5503f5edefe3ec95465cf755f1585e02b732a6fa907afe7c7177R67-L103
    // they removed it, for some reason
    private void packetReceived (final ClientboundLoginCompressionPacket packet) {
        if (packet.getThreshold() < 0) {
            session.setCompression(null);
        }
    }

    @Override
    public void packetSending (final PacketSendingEvent packetSendingEvent) {
        for (final SessionListener listener : listeners) {
            listener.packetSending(packetSendingEvent);
        }
    }

    @Override
    public void packetSent (final Session session, final Packet packet) {
        for (final SessionListener listener : listeners) {
            listener.packetSent(session, packet);
        }
    }

    @Override
    public void packetError (final PacketErrorEvent packetErrorEvent) {
        for (final SessionListener listener : listeners) {
            listener.packetError(packetErrorEvent);
        }
        packetErrorEvent.setSuppress(true); // fixes the ohio sus exploit
    }

    @Override
    public void disconnecting (final DisconnectingEvent disconnectingEvent) {
        for (final SessionListener listener : listeners) {
            listener.disconnecting(disconnectingEvent);
        }
    }

    @Override
    public void disconnected (final DisconnectedEvent disconnectedEvent) {
        loggedIn = false;

        disconnectedTimes++;

        final Throwable cause = disconnectedEvent.getCause();

        if (printDisconnectedCause && cause != null) logger.error(cause);

        if (!isTransferring) cookies.clear();

        int reconnectDelay = options.reconnectDelay;

        if (isTransferring) {
            // for now, it's going to transfer to the same server instead of
            // other servers

            reconnect(); // instantly reconnect

            isTransferring = false;
        } else {
            final String stringMessage = ComponentUtilities.stringify(disconnectedEvent.getReason());

            if (
                    stringMessage.equals("Wait 5 seconds before connecting, thanks! :)") ||
                            stringMessage.equals("You are logging in too fast, try again later.") ||
                            stringMessage.equals("Connection throttled! Please wait before reconnecting.")
            ) reconnectDelay = 1000 * (5 + 2); // 2 seconds extra delay just in case

            executor.schedule(this::reconnect, reconnectDelay, TimeUnit.MILLISECONDS);
        }

        for (final SessionListener listener : listeners) {
            try {
                listener.disconnected(disconnectedEvent);
            } catch (final Exception e) {
                logger.error(e);
            }
        }
    }

    public String getServerString () { return getServerString(false); }

    public String getServerString (final boolean bypassHidden) {
        return options.hidden && !bypassHidden ?
                options.serverName :
                host + ":" + port;
    }

    public void stop () {
        session.disconnect("Received stop signal");
        Main.bots.remove(this);
    }

    public void addListener (final Listener listener) {
        listeners.add(listener);
    }

    @Override
    public String toString () {
        return "Bot{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", loggedIn=" + loggedIn +
                '}';
    }

    public static class Listener extends SessionAdapter {
        public void connecting () { }

        public void loadedPlugins (final Bot bot) { }
    }
}
