package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.data.game.ClientCommand;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEventValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import net.kyori.adventure.text.Component;
import org.cloudburstmc.math.vector.Vector3i;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SelfCarePlugin extends Bot.Listener {
    private final Bot bot;

    private ScheduledFuture<?> checkTask;

    public boolean visibility = false;

    private int entityId;
    private GameMode gamemode;
    private int permissionLevel;
    private int positionPacketsPerSecond = 0;
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

        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public void systemMessageReceived(Component component, String string, String ansi) {
                if (string.equals("Successfully enabled CommandSpy")) cspy = true;
                else if (string.equals("Successfully disabled CommandSpy")) cspy = false;

                else if (string.equals("Vanish for " + bot.username + ": enabled")) vanish = true;
                else if (string.equals("You are now completely invisible to normal users, and hidden from in-game commands.")) vanish = true;
                else if (string.equals("Vanish for " + bot.username + ": disabled")) vanish = false;

                else if (string.equals("You no longer have a nickname.")) nickname = true;
                else if (string.startsWith("Your nickname is now ")) nickname = false;

                else if (string.equals("SocialSpy for " + bot.username + ": enabled")) socialspy = true;
                else if (string.equals("SocialSpy for " + bot.username + ": disabled")) socialspy = false;

                else if (string.startsWith("You have been muted")) muted = true;
                else if (string.equals("You have been unmuted.")) muted = false;

                else if (string.equals("You now have the tag: " + bot.config.selfCare.prefix.prefix)) prefix = true;
                else if (string.startsWith("You no longer have a tag")) prefix = false;
                else if (string.startsWith("You now have the tag: ")) prefix = false;

                else if (string.equals("Successfully set your username to \"" + bot.username + "\"")) username = true;
                else if (string.startsWith("Successfully set your username to \"")) username = false;
            }
        });

        bot.position.addListener(new PositionPlugin.Listener() {
            @Override
            public void positionChange(Vector3i position) {
                SelfCarePlugin.this.positionChange();
            }
        });
    }

    public void check () {
        final Configuration.SelfCare selfCares = bot.config.selfCare;

        // chat only
        if (selfCares.op && permissionLevel < 2) bot.chat.send("/minecraft:op @s[type=player]");
        else if (selfCares.gamemode && gamemode != GameMode.CREATIVE) bot.chat.send("/minecraft:gamemode creative @s[type=player]");
        else if (selfCares.cspy && !cspy && bot.options.kaboom) bot.chat.send("/commandspy:commandspy on");
        else if (selfCares.prefix.enabled && !prefix && bot.options.kaboom) bot.chat.send("/extras:prefix " + bot.config.selfCare.prefix.prefix);
        else if (selfCares.username && !username && bot.options.kaboom) bot.chat.send("/extras:username " + bot.username);

        // core
        // TODO: improve lol
        else if (selfCares.icu.enabled && positionPacketsPerSecond > selfCares.icu.positionPacketsPerSecond) bot.core.run("essentials:sudo * icu stop");
        else if (selfCares.vanish && !vanish && !visibility && bot.options.hasEssentials) {
            if (bot.options.useChat) bot.chat.sendCommandInstantly("essentials:vanish enable");
            else bot.core.run("essentials:vanish " + bot.username + " enable");
        }
        else if (selfCares.nickname && !nickname && bot.options.hasEssentials) {
            if (bot.options.useChat) bot.chat.sendCommandInstantly("essentials:nick off");
            else bot.core.run("essentials:nickname " + bot.username + " off");
        }
        else if (selfCares.socialspy && !socialspy && bot.options.hasEssentials) {
            if (bot.options.useChat) bot.chat.sendCommandInstantly("essentials:socialspy enable");
            else bot.core.run("essentials:socialspy " + bot.username + " enable");
        }
        else if (selfCares.mute && muted && bot.options.hasEssentials) {
            if (bot.options.useChat) bot.chat.sendCommandInstantly("essentials:mute " + bot.profile.getIdAsString());
            else bot.core.run("essentials:mute " + bot.profile.getIdAsString());

            muted = false; // too lazy fix and probably the worst fix?
        }
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
        else if (packet instanceof ClientboundGameEventPacket) packetReceived((ClientboundGameEventPacket) packet);
        else if (packet instanceof ClientboundEntityEventPacket) packetReceived((ClientboundEntityEventPacket) packet);
    }

    public void packetReceived (ClientboundLoginPacket packet) {
        this.entityId = packet.getEntityId();
        this.gamemode = packet.getGameMode();

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

    // totallynotskidded™ from smp.,.,
    public void positionChange () {
        positionPacketsPerSecond++;

        bot.executor.schedule(() -> positionPacketsPerSecond--, 1, TimeUnit.SECONDS);
    }

    @Override
    public void disconnected (DisconnectedEvent event) {
        checkTask.cancel(true);
    }
}
