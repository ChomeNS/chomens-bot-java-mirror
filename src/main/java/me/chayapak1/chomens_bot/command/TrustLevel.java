package me.chayapak1.chomens_bot.command;

import me.chayapak1.chomens_bot.Configuration;
import me.chayapak1.chomens_bot.Main;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public enum TrustLevel {
    PUBLIC(0, Component.text("Public").color(NamedTextColor.GREEN)),
    TRUSTED(1, Component.text("Trusted").color(NamedTextColor.RED)),
    ADMIN(2, Component.text("Admin").color(NamedTextColor.DARK_RED)),
    OWNER(3, Component.text("Owner").color(NamedTextColor.LIGHT_PURPLE));

    public final int level;
    public final Component component;

    TrustLevel (final int level, final Component component) {
        this.level = level;
        this.component = component;
    }

    public static TrustLevel fromDiscordRoles (final List<Role> roles) {
        if (Main.discord == null) return PUBLIC;

        final Configuration.Discord options = Main.discord.options;

        if (options == null) return PUBLIC;

        TrustLevel userTrustLevel = PUBLIC;

        for (final Role role : roles) {
            if (role.getName().equalsIgnoreCase(options.ownerRoleName)) {
                userTrustLevel = OWNER;
                break;
            } else if (role.getName().equalsIgnoreCase(options.adminRoleName)) {
                userTrustLevel = ADMIN;
                break;
            } else if (role.getName().equalsIgnoreCase(options.trustedRoleName)) {
                userTrustLevel = TRUSTED;
                break;
            }
        }

        return userTrustLevel;
    }
}
