package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TickPlugin implements Listener {
    private final Bot bot;

    public final AtomicLong lastTickTime = new AtomicLong();
    public final AtomicLong lastSecondTickTime = new AtomicLong();

    public TickPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);

        bot.executor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        bot.executor.scheduleAtFixedRate(this::tickLocalSecond, 0, 1, TimeUnit.SECONDS);
    }

    private void tick () {
        bot.listener.dispatch(listener -> {
            try {
                listener.onAlwaysTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in an always tick listener!");
                bot.logger.error(e);
            }
        });

        if (!bot.loggedIn) return;

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

    private void tickLocalSecond () {
        if (!bot.loggedIn) return;

        bot.listener.dispatch(listener -> {
            try {
                listener.onLocalSecondTick();
            } catch (final Throwable e) {
                bot.logger.error("Caught exception in a local second tick listener!");
                bot.logger.error(e);
            }
        });
    }

    @Override
    public void packetReceived (final Session session, final Packet packet) {
        if (packet instanceof final ClientboundSetTimePacket t_packet) packetReceived(t_packet);
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
}
