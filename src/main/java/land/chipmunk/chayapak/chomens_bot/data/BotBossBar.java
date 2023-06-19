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

    public Component secret = Component.text(Math.random() * 69420);

    private final Bot bot;

    public String id;
    public String players;
    public boolean visible;
    public long max;
    public int value;

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

    public void setTitle (Component title) {
        setTitle(title, false);
    }
    public void setTitle (Component title, boolean force) {
        if (ComponentUtilities.isEqual(this.title, title) && !force) return;

        this.title = title;

        bot.core().run("minecraft:bossbar set " + id + " name " + GsonComponentSerializer.gson().serialize(title));
    }

    public void setColor (BossBarColor color) {
        setColor(color, false);
    }
    public void setColor (BossBarColor color, boolean force) {
        if (this.color == color && !force) return;

        this.color = color;

        bot.core().run("minecraft:bossbar set " + id + " color " + (color == BossBarColor.LIME ? "green" : color.name().toLowerCase()));
    }

    public void setPlayers (String players) {
        if (this.players.equals(players)) return;

        this.players = players;

        bot.core().run("minecraft:bossbar set " + id + " players " + players);
    }

    public void setDivision (BossBarDivision division) {
        setDivision(division, false);
    }
    public void setDivision (BossBarDivision _division, boolean force) {
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

        bot.core().run("minecraft:bossbar set " + id + " style " + division);
    }

    public void setValue (int value) {
        setValue(value, false);
    }
    public void setValue (int value, boolean force) {
        if (this.value == value && !force) return;

        this.value = value;

        bot.core().run("minecraft:bossbar set " + id + " value " + value);
    }

    public void setVisible (boolean visible) {
        if (this.visible == visible) return;

        this.visible = visible;

        bot.core().run("minecraft:bossbar set " + id + " visible " + visible);
    }

    public void setMax (long max) {
        setMax(max, false);
    }
    public void setMax (long max, boolean force) {
        if (this.max == max && !force) return;

        this.max = max;

        bot.core().run("minecraft:bossbar set " + id + " max " + max);
    }
}
