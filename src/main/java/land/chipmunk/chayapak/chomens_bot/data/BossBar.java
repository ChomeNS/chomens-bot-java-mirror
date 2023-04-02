package land.chipmunk.chayapak.chomens_bot.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@AllArgsConstructor
public class BossBar {
    @Getter @Setter private Component name;
    @Getter @Setter private BossBarColor color;
    @Getter @Setter private long max;
    @Getter @Setter private String players;
    @Getter @Setter private BossBarStyle style;
    @Getter @Setter private int value;
    @Getter @Setter private boolean visible;
}

