package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.data.game.RemoteDebugSampleType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundDebugSamplePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundDebugSampleSubscriptionPacket;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TickPlugin implements Listener {
    private final Bot bot;

    public final AtomicLong lastTickTime = new AtomicLong();
    public final AtomicLong lastSecondTickTime = new AtomicLong();

    private boolean receivedDebugSample = false;
    private final AtomicLong lastDebugSubscriptionTime = new AtomicLong();

    public TickPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);

        bot.executor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(this::tickLocalSecond, 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void connected (final ConnectedEvent event) {
        resubscribeDebug();
    }

    private void tick () {
        if (!bot.loggedIn) return;

        bot.listener.dispatch(listener -> {
            try {
                listener.onLocalTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a local tick listener!");
                bot.logger.error(e);
            }
        });

        if (!receivedDebugSample || bot.selfCare.data.permissionLevel < 2) dispatchTick();
    }

    private void tickLocalSecond () {
        if (!bot.loggedIn) return;

        if (System.currentTimeMillis() - lastDebugSubscriptionTime.get() >= 5 * 1000) {
            resubscribeDebug();
            lastDebugSubscriptionTime.set(System.currentTimeMillis());
        }

        bot.listener.dispatch(listener -> {
            try {
                listener.onLocalSecondTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a local second tick listener!");
                bot.logger.error(e);
            }
        });
    }

    private void resubscribeDebug () {
        bot.session.send(new ServerboundDebugSampleSubscriptionPacket(RemoteDebugSampleType.TICK_TIME));
    }

    private void dispatchTick () {
        bot.listener.dispatch(listener -> {
            try {
                listener.onTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a tick listener!");
                bot.logger.error(e);
            }
        });

        lastTickTime.set(System.currentTimeMillis());
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundSetTimePacket t_packet) packetReceived(t_packet);
        else if (packet instanceof final ClientboundDebugSamplePacket t_packet) packetReceived(t_packet);
    }

    private void packetReceived (final ClientboundSetTimePacket ignoredPacket) {
        bot.listener.dispatch(listener -> {
            try {
                listener.onSecondTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a server time update listener!");
                bot.logger.error(e);
            }
        });

        lastSecondTickTime.set(System.currentTimeMillis());
    }

    private void packetReceived (final ClientboundDebugSamplePacket packet) {
        if (packet.getDebugSampleType() != RemoteDebugSampleType.TICK_TIME) return;

        receivedDebugSample = true;

        dispatchTick();
    }
}
