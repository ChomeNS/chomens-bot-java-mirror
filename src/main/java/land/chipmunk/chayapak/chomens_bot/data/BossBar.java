package land.chipmunk.chayapak.chomens_bot.data;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class BossBar {
    public UUID uuid;

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
}

