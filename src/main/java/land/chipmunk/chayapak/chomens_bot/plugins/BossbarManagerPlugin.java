package land.chipmunk.chayapak.chomens_bot.plugins;

import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.event.session.SessionAdapter;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.BossBar;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BossbarManagerPlugin extends CorePlugin.Listener {
    private final Bot bot;

    private ScheduledFuture<?> tickTask;

    private final Map<String, BossBar> bossBars = new HashMap<>();

    @Getter @Setter private String bossBarPrefix = "chomens_bot:";
    @Getter @Setter private String textDisplayPrefix = "chomens_bot_";

    public BossbarManagerPlugin (Bot bot) {
        this.bot = bot;

        bot.core().addListener(this);

        bot.addListener(new SessionAdapter() {
            @Override
            public void disconnected(DisconnectedEvent event) {
                tickTask.cancel(true);
            }
        });
    }

    @Override
    public void ready () {
        tickTask = bot.executor().scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
    }

    public void tick () {
        for (Map.Entry<String, BossBar> _bossBar : bossBars.entrySet()) {
            final String name = _bossBar.getKey();
            final BossBar bossBar = _bossBar.getValue();

            final String stringifiedComponent = GsonComponentSerializer.gson().serialize(bossBar.name());

            bot.core().run("minecraft:bossbar add " + bossBarPrefix + name + " \"\"");
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " players " + bossBar.players());
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " name " + stringifiedComponent);
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " color " + bossBar.color().color);
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " visible " + bossBar.visible());
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " style " + bossBar.style().style);
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " value " + bossBar.value());
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " max " + bossBar.max());

            // is there any way instead of using random?
            bot.core().run("minecraft:data modify entity @e[tag=" + textDisplayPrefix + name + ",limit=1,sort=random] text set value '" + stringifiedComponent.replace("'", "\\\'") + "'");
        }
    }

    public void add (String name, BossBar bossBar) {
        bossBars.put(name, bossBar);
    }

    public void remove (String name) {
        bossBars.remove(name);
        bot.core().run("minecraft:bossbar remove " + bossBarPrefix + name);
    }

    public BossBar get (String name) {
        return bossBars.get(name);
    }
}
