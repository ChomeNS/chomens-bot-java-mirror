package land.chipmunk.chayapak.chomens_bot.data;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.UUID;

public class BotBossBar extends BossBar {
    public UUID uuid = UUID.randomUUID(); // the random uuid will be temporary

    public final Component secret = Component.text(Math.random() * 69420);

    public boolean gotSecret = false;

    private final Bot bot;

    public String id;

    private String players;
    private boolean visible;
    private long max;
    private int value;

    public BotBossBar(
            Component title,
            String players,
            BossBarColor color,
            BossBarDivision division,
            boolean visible,
            long max,
            int value,
            Bot bot
    ) {
        super(null, title, color, division, value);
        this.players = players;
        this.visible = visible;
        this.max = max;
        this.bot = bot;
    }

    public Component title() {
        return title;
    }

    public void setTitle(Component title) {
        setTitle(title, false);
    }

    public void setTitle(Component title, boolean force) {
        if (!gotSecret) return;

        if (ComponentUtilities.isEqual(this.title, title) && !force) return;

        if (bot.bossbar.actionBar) {
            bot.chat.actionBar(title, players);

            return;
        }

        this.title = title;

        bot.core.run("minecraft:bossbar set " + id + " name " + GsonComponentSerializer.gson().serialize(title));
    }

    public BossBarColor color(BossBarColor color) {
        return color;
    }

    public void setColor(BossBarColor color) {
        setColor(color, false);
    }

    public void setColor(BossBarColor color, boolean force) {
        if (!gotSecret) return;

        if (this.color == color && !force) return;

        this.color = color;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " color " + (color == BossBarColor.LIME ? "green" : (color == BossBarColor.CYAN ? "blue" : color.name().toLowerCase())));
    }

    public String players() {
        return players;
    }

    public void setPlayers(String players) {
        if (!gotSecret) return;

        if (this.players.equals(players)) return;

        this.players = players;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " players " + players);
    }

    public BossBarDivision division () { return division; }
    public void setDivision (BossBarDivision division) {
        setDivision(division, false);
    }
    public void setDivision (BossBarDivision _division, boolean force) {
        if (!gotSecret) return;

        if (this.division == _division && !force) return;

        this.division = _division;

        String division = null;

        switch (_division) {
            case NONE -> division = "progress";
            case NOTCHES_20 -> division = "notched_20";
            case NOTCHES_6 -> division = "notched_6";
            case NOTCHES_12 -> division = "notched_12";
            case NOTCHES_10 -> division = "notched_10";
        }

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " style " + division);
    }

    public int value () { return value; }
    public void setValue (int value) {
        setValue(value, false);
    }
    public void setValue (int value, boolean force) {
        if (!gotSecret) return;

        if (this.value == value && !force) return;

        this.value = value;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " value " + value);
    }

    public boolean visible () { return visible; }
    public void setVisible (boolean visible) {
        if (!gotSecret) return;

        if (this.visible == visible) return;

        this.visible = visible;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " visible " + visible);
    }

    public long max () { return max; }
    public void setMax (long max) {
        setMax(max, false);
    }
    public void setMax (long max, boolean force) {
        if (!gotSecret) return;

        if (this.max == max && !force) return;

        this.max = max;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " max " + max);
    }
}
