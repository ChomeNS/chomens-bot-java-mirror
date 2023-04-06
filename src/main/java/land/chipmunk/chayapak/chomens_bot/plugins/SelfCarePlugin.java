package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
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
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Configuration;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SelfCarePlugin extends SessionAdapter {
    private final Bot bot;

    private ScheduledFuture<?> checkTask;

    @Getter @Setter boolean visibility = false;

    private int entityId;
    private GameMode gamemode;
    private int permissionLevel;
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

        bot.chat().addListener(new ChatPlugin.ChatListener() {
            @Override
            public void systemMessageReceived(String message, Component component) {
                if (message.equals("Successfully enabled CommandSpy")) cspy = true;
                else if (message.equals("Successfully disabled CommandSpy")) cspy = false;

                else if (message.equals("Vanish for " + bot.username() + ": enabled")) vanish = true;
                else if (message.equals("Vanish for " + bot.username() + ": disabled")) vanish = false;

                else if (message.equals("You no longer have a nickname.")) nickname = true;
                else if (message.startsWith("Your nickname is now ")) nickname = false;

                else if (message.equals("SocialSpy for " + bot.username() + ": enabled")) socialspy = true;
                else if (message.equals("SocialSpy for " + bot.username() + ": disabled")) socialspy = false;

                else if (message.startsWith("You have been muted")) muted = true;
                else if (message.equals("You have been unmuted.")) muted = false;

                else if (message.equals("You now have the tag: [ChomeNS Bot]") || // for 1.19.2 (or 1.19?) and older clones
                         message.equals("You now have the tag: &8[&eChomeNS Bot&8]")
                ) prefix = true;
                else if (message.startsWith("You no longer have a tag")) prefix = false;
                else if (message.startsWith("You now have the tag: ")) prefix = false;

                else if (message.equals("Successfully set your username to \"" + bot.username() + "\"")) username = true;
                else if (message.startsWith("Successfully set your username to \"")) username = false;
            }
        });
    }

    public void check () {
        final Configuration.SelfCare selfCares = bot.config().selfCare();

        if (selfCares.gamemode() && gamemode != GameMode.CREATIVE) bot.chat().send("/minecraft:gamemode creative @s[type=player]");
        else if (selfCares.op() && permissionLevel < 2) bot.chat().send("/minecraft:op @s[type=player]");
        else if (selfCares.cspy() && !cspy && bot.kaboom()) bot.chat().send("/commandspy:commandspy on");
        else if (selfCares.vanish() && !vanish && !visibility) bot.chat().send("/essentials:vanish enable");
        else if (selfCares.nickname() && !nickname) bot.chat().send("/essentials:nickname off");
        else if (selfCares.socialspy() && !socialspy) bot.chat().send("/essentials:socialspy enable");
        else if (selfCares.mute() && muted) bot.chat().send("/essentials:mute " + bot.username());
        else if (selfCares.prefix() && !prefix && bot.kaboom()) bot.chat().send("/extras:prefix &8[&eChomeNS Bot&8]");
        else if (selfCares.username() && !username && bot.kaboom()) bot.chat().send("/extras:username " + bot.username());
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

        final Runnable task = () -> {
            final Session session = bot.session();
            final PacketProtocol protocol = session.getPacketProtocol();
            if (
                    !session.isConnected() ||
                            (
                                    protocol instanceof MinecraftProtocol &&
                                            ((MinecraftProtocol) protocol).getState() != ProtocolState.GAME
                            )
            ) return;

            check();
        };

        checkTask = bot.executor().scheduleAtFixedRate(task, 0, bot.config().selfCare().checkInterval(), TimeUnit.MILLISECONDS);
    }

    public void packetReceived (ClientboundGameEventPacket packet) {
        final GameEvent notification = packet.getNotification();
        final GameEventValue value = packet.getValue();

        if (notification == GameEvent.ENTER_CREDITS) {
            bot.session().send(new ServerboundClientCommandPacket(ClientCommand.RESPAWN));
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

    @Override
    public void disconnected (DisconnectedEvent event) {
        checkTask.cancel(true);
    }
}
