package me.chayapak1.chomens_bot.data.bossbar;

import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class BossBar {
    public final UUID uuid;

    public Component title;
    public BossBarColor color;
    public BossBarDivision division;
    public float health;

    public BossBar (
            UUID uuid,
            Component title,
            BossBarColor color,
            BossBarDivision division,
            float health
    ) {
        this.uuid = uuid;
        this.title = title;
        this.color = color;
        this.division = division;
        this.health = health;
    }

    @Override
    public String toString() {
        return "BossBar{" +
                "uuid=" + uuid +
                ", title=" + title +
                ", color=" + color +
                ", division=" + division +
                ", health=" + health +
                '}';
    }
}
