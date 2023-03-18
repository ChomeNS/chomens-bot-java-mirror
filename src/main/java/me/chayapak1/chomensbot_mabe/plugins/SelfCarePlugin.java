package me.chayapak1.chomensbot_mabe.plugins;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.ProtocolState;
import com.github.steveice10.mc.protocol.data.game.entity.EntityEvent;
import com.github.steveice10.mc.protocol.data.game.entity.player.GameMode;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEvent;
import com.github.steveice10.mc.protocol.data.game.level.notify.GameEventValue;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundEntityEventPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundGameEventPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.packet.PacketProtocol;
import me.chayapak1.chomensbot_mabe.Bot;
import net.kyori.adventure.text.Component;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SelfCarePlugin extends SessionAdapter {
    private final Bot bot;

    private ScheduledFuture<?> futureTask;

    private int entityId;
    private GameMode gamemode;
    private int permissionLevel;
    private boolean cspy = false;
    private boolean vanish = false;
    private boolean socialspy = false;
    private boolean muted = false;
    private boolean prefix = false;

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

                else if (message.equals("SocialSpy for " + bot.username() + ": enabled")) socialspy = true;
                else if (message.equals("SocialSpy for " + bot.username() + ": disabled")) socialspy = false;

                else if (message.startsWith("You have been muted")) muted = true;
                else if (message.equals("You have been unmuted.")) muted = false;

                else if (message.equals("You now have the tag: [ChomeNS Bot]") || // for 1.19.2 (or 1.19?) and older clones
                         message.equals("You now have the tag: &8[&eChomeNS Bot&8]")
                ) {
                    prefix = true;
                    return;
                }
                if (message.startsWith("You no longer have a tag")) prefix = false;
                if (message.startsWith("You now have the tag: ")) prefix = false;
            }
        });
    }

    public void check () {
        if (gamemode != GameMode.CREATIVE) bot.chat().send("/minecraft:gamemode creative @s[type=player]");
        else if (permissionLevel < 2) bot.chat().send("/minecraft:op @s[type=player]");
        else if (!cspy) bot.chat().send("/commandspy:commandspy on");
        else if (!vanish) bot.chat().send("/essentials:vanish enable");
        else if (!socialspy) bot.chat().send("/essentials:socialspy enable");
        else if (muted) bot.chat().send("/essentials:mute " + bot.username());
        else if (!prefix) bot.chat().send("/extras:prefix &8[&eChomeNS Bot&8]");
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

        futureTask = bot.executor().scheduleAtFixedRate(task, 50, 500, TimeUnit.MILLISECONDS);
    }

    public void packetReceived (ClientboundGameEventPacket packet) {
        final GameEvent notification = packet.getNotification();
        final GameEventValue value = packet.getValue();

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
        futureTask.cancel(true);
    }
}
