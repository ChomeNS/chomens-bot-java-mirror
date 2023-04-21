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

public class BossbarManagerPlugin extends SessionAdapter {
    private final Bot bot;

    private ScheduledFuture<?> tickTask;
    private ScheduledFuture<?> updateTask;

    private final Map<String, BossBar> bossBars = new HashMap<>();

    @Getter @Setter private boolean enabled = true;
    @Getter @Setter private boolean actionbar = false;

    @Getter @Setter private String bossBarPrefix = "chomens_bot:";
    @Getter @Setter private String textDisplayPrefix = "chomens_bot_";

    public BossbarManagerPlugin (Bot bot) {
        this.bot = bot;

        bot.core().addListener(new CorePlugin.Listener() {
            @Override
            public void ready() {
                BossbarManagerPlugin.this.ready();
            }
        });

        bot.addListener(this);
    }

    public void ready () {
        tickTask = bot.executor().scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);
        updateTask = bot.executor().scheduleAtFixedRate(this::update, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void disconnected(DisconnectedEvent event) {
        tickTask.cancel(true);
        updateTask.cancel(true);
    }

    private void update() {
        if (!enabled || actionbar) return;
        for (Map.Entry<String, BossBar> _bossBar : bossBars.entrySet()) {
            final String name = _bossBar.getKey();
            final BossBar bossBar = _bossBar.getValue();
            createBossBar(name, bossBar.players());
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " color " + bossBar.color().color);
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " visible " + bossBar.visible());
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " style " + bossBar.style().style);
            bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " max " + bossBar.max());
        }
    }

    public void tick () {
        if (!enabled) return;
        for (Map.Entry<String, BossBar> _bossBar : bossBars.entrySet()) {
            final String name = _bossBar.getKey();
            final BossBar bossBar = _bossBar.getValue();

            final String stringifiedComponent = GsonComponentSerializer.gson().serialize(bossBar.name());

            if (!actionbar) {
                bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " name " + stringifiedComponent);
                bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " value " + bossBar.value());
            } else {
                bot.core().run("minecraft:title " + bossBar.players() + " actionbar " + stringifiedComponent);
            }

            // is there any way instead of using random?
            bot.core().run("minecraft:data modify entity @e[tag=" + textDisplayPrefix + name + ",limit=1,sort=random] text set value '" + stringifiedComponent.replace("'", "\\\'") + "'");
        }
    }

    private void createBossBar (String name, String players) {
        if (!enabled || actionbar) return;
        bot.core().run("minecraft:bossbar add " + bossBarPrefix + name + " \"\"");
        bot.core().run("minecraft:bossbar set " + bossBarPrefix + name + " players " + players);
    }

    public void add (String name, BossBar bossBar) {
        bossBars.put(name, bossBar);
        createBossBar(name, bossBar.players());
    }

    public void remove (String name) {
        bossBars.remove(name);
        bot.core().run("minecraft:bossbar remove " + bossBarPrefix + name);
    }

    public BossBar get (String name) {
        return bossBars.get(name);
    }
}
