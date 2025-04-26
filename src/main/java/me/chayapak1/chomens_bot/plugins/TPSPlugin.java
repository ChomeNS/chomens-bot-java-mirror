package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.bossbar.BotBossBar;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.network.event.session.ConnectedEvent;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;

import java.text.DecimalFormat;
import java.util.Arrays;

// totallynotskidded™ from meteor client (https://github.com/MeteorDevelopment/meteor-client/blob/master/src/main/java/meteordevelopment/meteorclient/utils/world/TickRate.java)
public class TPSPlugin implements Listener {
    private final Bot bot;

    private boolean enabled = false;

    private final double[] tickRates = new double[20];
    private int nextIndex = 0;
    private long timeLastTimeUpdate = -1;
    private long timeGameJoined;

    private final String bossbarName = "tpsbar";

    public TPSPlugin (final Bot bot) {
        this.bot = bot;

        bot.listener.addListener(this);
    }

    private void createBossBar () {
        final BotBossBar bossBar = new BotBossBar(
                Component.empty(),
                "@a",
                getBossBarColor(getTickRate()),
                BossBarDivision.NOTCHES_20,
                true,
                20,
                0,
                bot
        );

        bot.bossbar.add(bossbarName, bossBar);
    }

    public void on () {
        if (enabled) return;

        enabled = true;

        createBossBar();
    }

    public void off () {
        enabled = false;

        final BotBossBar bossBar = bot.bossbar.get(bossbarName);

        if (bossBar != null) bossBar.setTitle(Component.text("TPSBar is currently disabled"));

        bot.bossbar.remove(bossbarName);
    }

    @Override
    public void onTick () {
        updateTPSBar();
    }

    private void updateTPSBar () {
        if (!enabled) return;

        try {
            final double tickRate = getTickRate();

            final DecimalFormat formatter = new DecimalFormat("##.##");

            final Component component = Component.translatable(
                    "%s - %s",
                    Component.text("TPS").color(NamedTextColor.GRAY),
                    Component.text(formatter.format(tickRate)).color(getColor(tickRate))
            ).color(NamedTextColor.DARK_GRAY);

            final BotBossBar bossBar = bot.bossbar.get(bossbarName);

            if (bossBar == null) {
                createBossBar();
                return;
            }

            bossBar.setTitle(component);
            bossBar.setColor(getBossBarColor(tickRate));
            bossBar.setValue((int) Math.round(tickRate));
        } catch (final Exception e) {
            bot.logger.error(e);
        }
    }

    private NamedTextColor getColor (final double tickRate) {
        if (tickRate > 15) return NamedTextColor.GREEN;
        else if (tickRate == 15) return NamedTextColor.YELLOW;
        else if (tickRate < 15 && tickRate > 10) return NamedTextColor.RED;
        else return NamedTextColor.DARK_RED;
    }

    private BossBarColor getBossBarColor (final double tickRate) {
        if (tickRate > 15) return BossBarColor.LIME;
        else if (tickRate == 15) return BossBarColor.YELLOW;
        else if (tickRate < 15 && tickRate > 10) return BossBarColor.RED;
        else return BossBarColor.PURPLE;
    }

    // this is the server time update tick, sent straight from server, not local!
    @Override
    public void onSecondTick () {
        final long now = System.currentTimeMillis();
        final float timeElapsed = (float) (now - timeLastTimeUpdate) / 1000.0F;
        tickRates[nextIndex] = MathUtilities.clamp(20.0f / timeElapsed, 0.0f, 20.0f);
        nextIndex = (nextIndex + 1) % tickRates.length;
        timeLastTimeUpdate = now;
    }

    @Override
    public void connected (final ConnectedEvent event) {
        Arrays.fill(tickRates, 0);
        nextIndex = 0;
        timeGameJoined = timeLastTimeUpdate = System.currentTimeMillis();
    }

    public double getTickRate () {
        if (System.currentTimeMillis() - timeGameJoined < 4000) return 20;

        int numTicks = 0;
        float sumTickRates = 0.0f;
        for (final double tickRate : tickRates) {
            if (tickRate > 0) {
                sumTickRates += (float) tickRate;
                numTicks++;
            }
        }
        return sumTickRates / numTicks;
    }
}
