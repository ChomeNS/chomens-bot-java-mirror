package me.chayapak1.chomens_bot.data.bossbar;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarColor;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarDivision;

import java.util.UUID;

public class BossBar {
    public final UUID uuid;

    public Component title;
    public BossBarColor color;
    public BossBarDivision division;
    public float health;

    public BossBar (
            final UUID uuid,
            final Component title,
            final BossBarColor color,
            final BossBarDivision division,
            final float health
    ) {
        this.uuid = uuid;
        this.title = title;
        this.color = color;
        this.division = division;
        this.health = health;
    }
}
