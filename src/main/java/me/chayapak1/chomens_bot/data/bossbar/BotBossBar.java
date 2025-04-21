package me.chayapak1.chomens_bot.data.bossbar;

import me.chayapak1.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;

import java.util.UUID;

public class BotBossBar extends BossBar {
    public UUID uuid;

    public final Component secret = Component
            .translatable(
                    "",
                    Component.text(UUID.randomUUID().toString())
            );

    public boolean gotSecret = false;

    private final Bot bot;

    public String onlyName;
    public String id;

    private String players;
    private boolean visible;
    private long max;
    private int value;

    public BotBossBar (
            final Component title,
            final String players,
            final BossBarColor color,
            final BossBarDivision division,
            final boolean visible,
            final long max,
            final int value,
            final Bot bot
    ) {
        super(null, title, color, division, value);
        this.players = players;
        this.visible = visible;
        this.max = max;
        this.bot = bot;
    }

    public Component title () {
        return title;
    }

    public void setTitle (final Component title) {
        setTitle(title, false);
    }

    public void setTitle (final Component title, final boolean force) {
        if ((this.title.equals(title) || !gotSecret) && !force) return;

        if (bot.bossbar.actionBar) {
            bot.chat.actionBar(title, players);

            return;
        }

        this.title = title;

        final String serialized = GsonComponentSerializer.gson().serialize(title);

        bot.core.run("minecraft:bossbar set " + id + " name " + serialized);

        if (!bot.core.hasRateLimit())
            bot.core.run("minecraft:execute as @e[type=minecraft:text_display,tag=" + bot.config.namespace + "_" + onlyName + "] run data modify entity @s text set value '" + serialized.replace("\\", "\\\\").replace("'", "\\'") + "'");
    }

    public BossBarColor color (final BossBarColor color) {
        return color;
    }

    public void setColor (final BossBarColor color) {
        setColor(color, false);
    }

    public void setColor (final BossBarColor color, final boolean force) {
        if ((this.color == color || !gotSecret) && !force) return;

        this.color = color;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " color " + (color == BossBarColor.LIME ? "green" : (color == BossBarColor.CYAN ? "blue" : color.name().toLowerCase())));
    }

    public String players () {
        return players;
    }

    public void setPlayers (final String players) { setPlayers(players, false); }

    public void setPlayers (final String players, final boolean force) {
        if ((this.players.equals(players) || !gotSecret) && !force) return;

        this.players = players;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " players " + players);
    }

    public BossBarDivision division () { return division; }

    public void setDivision (final BossBarDivision division) {
        setDivision(division, false);
    }

    public void setDivision (final BossBarDivision _division, final boolean force) {
        if ((this.division == _division || !gotSecret) && !force) return;

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

    public void setValue (final int value) {
        setValue(value, false);
    }

    public void setValue (final int value, final boolean force) {
        if ((this.value == value || !gotSecret) && !force) return;

        this.value = value;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " value " + value);
    }

    public boolean visible () { return visible; }

    public void setVisible (final boolean visible) {
        setVisible(visible, false);
    }

    public void setVisible (final boolean visible, final boolean force) {
        if ((this.visible == visible || !gotSecret) && !force) return;

        this.visible = visible;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " visible " + visible);
    }

    public long max () { return max; }

    public void setMax (final long max) {
        setMax(max, false);
    }

    public void setMax (final long max, final boolean force) {
        if ((this.max == max || !gotSecret) && !force) return;

        this.max = max;

        if (bot.bossbar.actionBar) return;

        bot.core.run("minecraft:bossbar set " + id + " max " + max);
    }
}
