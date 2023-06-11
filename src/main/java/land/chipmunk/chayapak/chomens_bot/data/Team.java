package land.chipmunk.chayapak.chomens_bot.data;

import com.github.steveice10.mc.protocol.data.game.scoreboard.CollisionRule;
import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.List;

@AllArgsConstructor
public class Team {
    @Getter @Setter private String teamName;
    @Getter private List<String> players;
    @Getter @Setter private Component displayName;
    @Getter @Setter private boolean friendlyFire;
    @Getter @Setter private boolean seeFriendlyInvisibles;
    @Getter @Setter private NameTagVisibility nametagVisibility;
    @Getter @Setter private CollisionRule collisionRule;
    @Getter @Setter private TeamColor color;
    @Getter @Setter private Component prefix;
    @Getter @Setter private Component suffix;

}
