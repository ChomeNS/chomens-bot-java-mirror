package me.chayapak1.chomens_bot.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
}
