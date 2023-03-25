package me.chayapak1.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import com.github.steveice10.packetlib.packet.Packet;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.util.NumberUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class TPSPlugin extends SessionAdapter {
    private final Bot bot;

    private boolean enabled = false;

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    private final String bossbarName = "chomens_bot:tpsbar";

    public TPSPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                updateTPSBar();
            }
        }, 1, 50);
    }

    public void on () {
        enabled = true;
    }

    public void off () {
        enabled = false;
        bot.core().run("minecraft:bossbar remove " + bossbarName);
    }

    private void updateTPSBar () {
        if (!enabled) return;

        final float floatTickRate = getTickRate();
        final double tickRate = Math.round(floatTickRate);

        final Component component = Component.translatable(
                "TPS - %s",
                Component.text(tickRate).color(NamedTextColor.GREEN)
        ).color(NamedTextColor.GRAY);

        // TODO: move these to like a bossbar manager of some sort
        bot.core().run("minecraft:bossbar add " + bossbarName + " \"\"");
        bot.core().run("minecraft:bossbar set " + bossbarName + " players @a");
        bot.core().run("minecraft:bossbar set " + bossbarName + " name " + GsonComponentSerializer.gson().serialize(component));
        bot.core().run("minecraft:bossbar set " + bossbarName + " color yellow");
        bot.core().run("minecraft:bossbar set " + bossbarName + " visible true");
        bot.core().run("minecraft:bossbar set " + bossbarName + " style notched_20");
        bot.core().run("minecraft:bossbar set " + bossbarName + " value " + (int) tickRate);
        bot.core().run("minecraft:bossbar set " + bossbarName + " max 20");
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundSetTimePacket) packetReceived((ClientboundSetTimePacket) packet);
        else if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
    }

    public void packetReceived (ClientboundSetTimePacket ignoredPacket) {
        long now = System.currentTimeMillis();
        float timeElapsed = (float) (now - timeLastTimeUpdate) / 1000.0F;
        tickRates[nextIndex] = NumberUtilities.clamp(20.0f / timeElapsed, 0.0f, 20.0f);
        nextIndex = (nextIndex + 1) % tickRates.length;
        timeLastTimeUpdate = now;
    }

    public void packetReceived (ClientboundLoginPacket ignoredPacket) {
        Arrays.fill(tickRates, 0);
        nextIndex = 0;
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    public float getTickRate() {
        if (System.currentTimeMillis() - timeGameJoined < 4000) return 20;

        int numTicks = 0;
        float sumTickRates = 0.0f;
        for (float tickRate : tickRates) {
            if (tickRate > 0) {
                sumTickRates += tickRate;
                numTicks++;
            }
        }
        return sumTickRates / numTicks;
    }
}
