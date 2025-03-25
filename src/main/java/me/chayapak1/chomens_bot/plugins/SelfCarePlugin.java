package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Configuration;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3d;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.ClientCommand;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EntityEvent;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode;
import org.geysermc.mcprotocollib.protocol.data.game.entity.player.PlayerState;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEvent;
import org.geysermc.mcprotocollib.protocol.data.game.level.notify.GameEventValue;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundSetPassengersPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundOpenScreenPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClosePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerCommandPacket;

import java.util.Arrays;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SelfCarePlugin extends Bot.Listener implements ChatPlugin.Listener, PositionPlugin.Listener, TickPlugin.Listener {
    private final Bot bot;

    private ScheduledFuture<?> checkTask;

    public boolean visibility = false;

    private int entityId;
    public GameMode gamemode;
    public int permissionLevel;

    private int positionPacketsPerSecond = 0;
    private long usernameStartTime = System.currentTimeMillis();

    private boolean cspy = false;
    private boolean vanish = false;
    private boolean nickname = false;
    private boolean socialspy = false;
    private boolean muted = false;
    private boolean prefix = false;
    private boolean username = true;

    public SelfCarePlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);
        bot.chat.addListener(this);
        bot.position.addListener(this);
        bot.tick.addListener(this);
    }

    @Override
    public void onSecondTick () {
        positionPacketsPerSecond = 0;
    }

    @Override
    public boolean systemMessageReceived(Component component, String string, String ansi) {
        if (string.equals("Successfully enabled CommandSpy")) cspy = true;
        else if (string.equals("Successfully disabled CommandSpy")) cspy = false;

        else if (string.equals(String.format(bot.options.essentialsMessages.vanishEnable1, bot.username))) vanish = true;
        else if (string.equals(bot.options.essentialsMessages.vanishEnable2)) vanish = true;
        else if (string.equals(String.format(bot.options.essentialsMessages.vanishDisable, bot.username))) vanish = false;

        else if (string.equals(bot.options.essentialsMessages.nickNameRemove)) nickname = true;
        else if (string.startsWith(bot.options.essentialsMessages.nickNameSet)) nickname = false;

        else if (string.equals(bot.options.essentialsMessages.socialSpyEnable)) socialspy = true;
        else if (string.equals(bot.options.essentialsMessages.socialSpyDisable)) socialspy = false;

        else if (string.startsWith(bot.options.essentialsMessages.muted)) muted = true;
        else if (string.equals(bot.options.essentialsMessages.unmuted)) muted = false;

        else if (string.equals("You now have the tag: " + bot.config.selfCare.prefix.prefix)) prefix = true;
        else if (string.startsWith("You no longer have a tag")) prefix = false;
        else if (string.startsWith("You now have the tag: ")) prefix = false;
            // prefix = true as a workaround to prevent spamming
        else if (string.equals("Something went wrong while saving the prefix. Please check console.")) prefix = true; // Parker2991

        else if (string.equals("Successfully set your username to \"" + bot.username + "\"")) username = true;
        else if (string.startsWith("Successfully set your username to \"")) {
            username = false;
            usernameStartTime = System.currentTimeMillis();
        }
        else if (string.startsWith("You already have the username \"" + bot.username + "\"")) username = true;
        else if (string.startsWith("You already have the username \"")) username = false;

        return true;
    }

    public void check () {
        final Configuration.SelfCare selfCares = bot.config.selfCare;

        final boolean kaboom = bot.serverFeatures.hasExtras;
        final boolean hasEssentials = bot.serverFeatures.hasEssentials;

        final boolean creayun = bot.options.creayun;

        // chat only
        if (selfCares.op && permissionLevel < 2) bot.chat.send("/minecraft:op @s[type=player]");
        else if (selfCares.gamemode && gamemode != GameMode.CREATIVE && !bot.options.creayun) bot.chat.send("/minecraft:gamemode creative @s[type=player]");

        // core
        else if (selfCares.cspy && !cspy && kaboom) {
            if (bot.options.useChat || !bot.options.coreCommandSpy) bot.chat.sendCommandInstantly("commandspy:commandspy on");
            else bot.core.run("commandspy:commandspy " + bot.username + " on");
        }

        // back to chat only
        else if (selfCares.prefix.enabled && !prefix && kaboom && !bot.options.creayun) bot.chat.send("/extras:prefix " + bot.config.selfCare.prefix.prefix);
        else if (selfCares.username && (System.currentTimeMillis() - usernameStartTime) >= 2 * 1000 && !username && kaboom) bot.chat.send("/extras:username " + bot.username);

        // core
        else if (selfCares.icu.enabled && positionPacketsPerSecond > selfCares.icu.positionPacketsPerSecond) bot.core.run("essentials:sudo * icu stop");
        else if (hasEssentials) {
            final String usernameOrBlank = !bot.options.useChat ?
                    bot.username + " " :
                    "";

            if (selfCares.vanish && !vanish && !visibility && !creayun) {
                runEssentialsCommand("essentials:vanish " + usernameOrBlank + "enable");
            } else if (selfCares.nickname && !nickname) {
                runEssentialsCommand("essentials:nickname " + usernameOrBlank + "off");
            } else if (selfCares.socialspy && !socialspy && !creayun) {
                runEssentialsCommand("essentials:socialspy " + usernameOrBlank + "enable");
            } else if (selfCares.mute && muted && !creayun) {
                runEssentialsCommand("essentials:mute " + bot.profile.getIdAsString());
                muted = false;
            }
        }
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
        else if (packet instanceof ClientboundGameEventPacket) packetReceived((ClientboundGameEventPacket) packet);
        else if (packet instanceof ClientboundEntityEventPacket) packetReceived((ClientboundEntityEventPacket) packet);
        else if (packet instanceof ClientboundOpenScreenPacket) packetReceived((ClientboundOpenScreenPacket) packet);
        else if (packet instanceof ClientboundSetPassengersPacket) packetReceived((ClientboundSetPassengersPacket) packet);
    }

    public void packetReceived (ClientboundLoginPacket packet) {
        this.entityId = packet.getEntityId();
        this.gamemode = packet.getCommonPlayerSpawnInfo().getGameMode();

        cspy = false;
        vanish = false;
        nickname = false;
        socialspy = false;
        muted = false;
        prefix = false;
        positionPacketsPerSecond = 0;

        final Runnable task = () -> {
            if (!bot.loggedIn) return;

            check();
        };

        checkTask = bot.executor.scheduleAtFixedRate(task, 0, bot.config.selfCare.delay, TimeUnit.MILLISECONDS);
    }

    public void packetReceived (ClientboundGameEventPacket packet) {
        final GameEvent notification = packet.getNotification();
        final GameEventValue value = packet.getValue();

        if (notification == GameEvent.ENTER_CREDITS && bot.config.selfCare.endCredits) {
            bot.session.send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
            return;
        }

        if (notification == GameEvent.CHANGE_GAMEMODE) gamemode = (GameMode) value;
    }

    public void packetReceived (ClientboundEntityEventPacket packet) {
        final EntityEvent event = packet.getEvent();
        final int id = packet.getEntityId();

        if (id != entityId) return;

        if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_0) permissionLevel = 0;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_1) permissionLevel = 1;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_2) permissionLevel = 2;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_3) permissionLevel = 3;
        else if (event == EntityEvent.PLAYER_OP_PERMISSION_LEVEL_4) permissionLevel = 4;
    }

    public void packetReceived (ClientboundOpenScreenPacket packet) {
        // instantly closes the window when received the packet
        // also should this be in self care?
        bot.session.send(
                new ServerboundContainerClosePacket(
                        packet.getContainerId()
                )
        );
    }

    public void packetReceived (ClientboundSetPassengersPacket packet) {
        if (
                Arrays.stream(packet.getPassengerIds())
                        .noneMatch(id -> id == entityId)
        ) return;

        // automatically unmount when mounting
        // e.g. player `/ride`s you
        // the server will automatically dismount for you,
        // so that's why we don't have to send PlayerState.STOP_SNEAKING
        bot.session.send(
                new ServerboundPlayerCommandPacket(
                        entityId,
                        PlayerState.START_SNEAKING
                )
        );
    }

    @Override
    public void positionChange(Vector3d position) {
        positionPacketsPerSecond++;
    }

    private void runEssentialsCommand (String command) {
        if (bot.options.useChat) bot.chat.sendCommandInstantly(command);
        else bot.core.run(command);
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        checkTask.cancel(true);
    }
}
