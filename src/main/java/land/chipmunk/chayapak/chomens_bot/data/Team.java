package land.chipmunk.chayapak.chomens_bot.data;

import com.github.steveice10.mc.protocol.data.game.scoreboard.CollisionRule;
import com.github.steveice10.mc.protocol.data.game.scoreboard.NameTagVisibility;
import com.github.steveice10.mc.protocol.data.game.scoreboard.TeamColor;
import net.kyori.adventure.text.Component;

public class Team {
    public String teamName;
    public Component displayName;
    public boolean friendlyFire;
    public boolean seeFriendlyInvisibles;
    public NameTagVisibility nametagVisibility;
    public CollisionRule collisionRule;
    public TeamColor color;
    public Component prefix;
    public Component suffix;

    public Team (
            String teamName,
            Component displayName,
            boolean friendlyFire,
            boolean seeFriendlyInvisibles,
            NameTagVisibility nametagVisibility,
            CollisionRule collisionRule,
            TeamColor color,
            Component prefix,
            Component suffix
    ) {
        this.teamName = teamName;
        this.displayName = displayName;
        this.friendlyFire = friendlyFire;
        this.seeFriendlyInvisibles = seeFriendlyInvisibles;
        this.nametagVisibility = nametagVisibility;
        this.collisionRule = collisionRule;
        this.color = color;
        this.prefix = prefix;
        this.suffix = suffix;
    }
}
