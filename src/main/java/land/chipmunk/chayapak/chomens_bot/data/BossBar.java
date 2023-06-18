package land.chipmunk.chayapak.chomens_bot.data;

import com.github.steveice10.mc.protocol.data.game.BossBarColor;
import com.github.steveice10.mc.protocol.data.game.BossBarDivision;
import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@AllArgsConstructor
public class BossBar {
    public UUID uuid;

    public Component title;
    public BossBarColor color;
    public BossBarDivision division;
    public float health;
}

