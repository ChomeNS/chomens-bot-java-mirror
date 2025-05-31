package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;

public enum TrustLevel {
    PUBLIC(0, Component.text(I18nUtilities.get("trust_level.public"), NamedTextColor.GREEN)),
    TRUSTED(1, Component.text(I18nUtilities.get("trust_level.trusted")).color(NamedTextColor.RED)),
    ADMIN(2, Component.text(I18nUtilities.get("trust_level.admin")).color(NamedTextColor.DARK_RED)),
    OWNER(3, Component.text(I18nUtilities.get("trust_level.owner")).color(NamedTextColor.LIGHT_PURPLE));

    public static final TrustLevel MAX = values()[values().length - 1];

    public final int level;
    public final Component component;

    TrustLevel (final int level, final Component component) {
        this.level = level;
        this.component = component;
    }

    public static TrustLevel fromDiscordRoles (final List<Role> roles) {
        if (Main.discord == null || Main.discord.options == null) return PUBLIC;

        final Configuration.Discord options = Main.discord.options;

        final Map<String, TrustLevel> roleToLevel = Map.of(
                options.ownerRoleName.toLowerCase(), OWNER,
                options.adminRoleName.toLowerCase(), ADMIN,
                options.trustedRoleName.toLowerCase(), TRUSTED
        );

        for (final Role role : roles) {
            final TrustLevel level = roleToLevel.get(role.getName().toLowerCase());
            if (level != null) return level;
        }

        return PUBLIC;
    }
}
