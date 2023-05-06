package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetTimePacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.packet.Packet;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BossBar;
import land.chipmunk.chayapak.chomens_bot.data.BossBarColor;
import land.chipmunk.chayapak.chomens_bot.data.BossBarStyle;
import land.chipmunk.chayapak.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;

public class TPSPlugin extends Bot.Listener {
    private final Bot bot;

    private boolean enabled = false;

    private final float[] tickRates = new float[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    private final String bossbarName = "tpsbar";

    public TPSPlugin (Bot bot) {
        this.bot = bot;

        bot.addListener(this);

        bot.core().addListener(new CorePlugin.Listener() {
               @Override
               public void ready() {
                   bot.tick().addListener(new TickPlugin.Listener() {
                       @Override
                       public void onTick() {
                           updateTPSBar();
                       }
                   });
               }
           }
        );
    }

    public void on () {
        enabled = true;
        bot.bossbar().add(bossbarName, new BossBar(
                Component.empty(),
                BossBarColor.WHITE,
                0,
                "",
                BossBarStyle.PROGRESS,
                0,
                false
        ));
    }

    public void off () {
        enabled = false;
        bot.bossbar().remove(bossbarName);
    }

    private void updateTPSBar () {
        if (!enabled) return;

        final float floatTickRate = getTickRate();
        final double tickRate = Math.round(floatTickRate);

        final Component component = Component.translatable(
                "TPS - %s",
                Component.text(tickRate).color(NamedTextColor.GREEN)
        ).color(NamedTextColor.GRAY);

        final BossBar bossBar = bot.bossbar().get(bossbarName);

        bossBar.players("@a");
        bossBar.name(component);
        bossBar.color(BossBarColor.YELLOW);
        bossBar.visible(true);
        bossBar.style(BossBarStyle.NOTCHED_20);
        bossBar.value((int) tickRate);
        bossBar.max(20);
    }

    @Override
    public void packetReceived (Session session, Packet packet) {
        if (packet instanceof ClientboundSetTimePacket) packetReceived((ClientboundSetTimePacket) packet);
        else if (packet instanceof ClientboundLoginPacket) packetReceived((ClientboundLoginPacket) packet);
    }

    public void packetReceived (ClientboundSetTimePacket ignoredPacket) {
        long now = System.currentTimeMillis();
        float timeElapsed = (float) (now - timeLastTimeUpdate) / 1000.0F;
        tickRates[nextIndex] = MathUtilities.clamp(20.0f / timeElapsed, 0.0f, 20.0f);
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
