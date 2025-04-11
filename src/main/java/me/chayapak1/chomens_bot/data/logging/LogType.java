package me.chayapak1.chomens_bot.data.logging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum LogType {
    INFO(Component.text("Info").color(NamedTextColor.GREEN)),
    CHAT(Component.text("Chat").color(NamedTextColor.GOLD)),
    TRUSTED_BROADCAST(Component.text("Trusted Broadcast").color(NamedTextColor.AQUA)),
    ERROR(Component.text("Error").color(NamedTextColor.RED)),
    COMMAND_OUTPUT(Component.text("Command Output").color(NamedTextColor.LIGHT_PURPLE)),
    AUTH(Component.text("Auth").color(NamedTextColor.RED)),
    SIMPLE_VOICE_CHAT(Component.text("Simple Voice Chat").color(NamedTextColor.AQUA)),
    DISCORD(Component.text("Discord").color(NamedTextColor.BLUE));

    public final Component component;

    LogType (final Component component) {
        this.component = component;
    }
}
