package me.chayapak1.chomens_bot.data;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.CollisionRule;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.NameTagVisibility;
import org.geysermc.mcprotocollib.protocol.data.game.scoreboard.TeamColor;
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

    public Style colorToStyle () {
        return switch (color) {
            case BLACK -> Style.style(NamedTextColor.BLACK);
            case DARK_BLUE -> Style.style(NamedTextColor.DARK_BLUE);
            case DARK_GREEN -> Style.style(NamedTextColor.DARK_GREEN);
            case DARK_AQUA -> Style.style(NamedTextColor.DARK_AQUA);
            case DARK_RED -> Style.style(NamedTextColor.DARK_RED);
            case DARK_PURPLE -> Style.style(NamedTextColor.DARK_PURPLE);
            case GOLD -> Style.style(NamedTextColor.GOLD);
            case GRAY -> Style.style(NamedTextColor.GRAY);
            case DARK_GRAY -> Style.style(NamedTextColor.DARK_GRAY);
            case BLUE -> Style.style(NamedTextColor.BLUE);
            case GREEN -> Style.style(NamedTextColor.GREEN);
            case AQUA -> Style.style(NamedTextColor.AQUA);
            case RED -> Style.style(NamedTextColor.RED);
            case LIGHT_PURPLE -> Style.style(NamedTextColor.LIGHT_PURPLE);
            case YELLOW -> Style.style(NamedTextColor.YELLOW);
            case WHITE -> Style.style(NamedTextColor.WHITE);
            case OBFUSCATED -> Style.style(TextDecoration.OBFUSCATED);
            case BOLD -> Style.style(TextDecoration.BOLD);
            case STRIKETHROUGH -> Style.style(TextDecoration.STRIKETHROUGH);
            case UNDERLINED -> Style.style(TextDecoration.UNDERLINED);
            case ITALIC -> Style.style(TextDecoration.ITALIC);
            case RESET -> Style.empty();
        };
    }
}
