package me.chayapak1.chomens_bot.data.logging;

import me.chayapak1.chomens_bot.util.I18nUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum LogType {
    INFO(Component.translatable(I18nUtilities.get("log_type.info"), NamedTextColor.GREEN)),
    CHAT(Component.translatable(I18nUtilities.get("log_type.chat"), NamedTextColor.GOLD)),
    TRUSTED_BROADCAST(Component.translatable(I18nUtilities.get("log_type.trusted_broadcast"), NamedTextColor.AQUA)),
    ERROR(Component.translatable(I18nUtilities.get("log_type.error"), NamedTextColor.RED)),
    COMMAND_OUTPUT(Component.translatable(I18nUtilities.get("log_type.command_output"), NamedTextColor.LIGHT_PURPLE)),
    AUTH(Component.translatable(I18nUtilities.get("log_type.auth"), NamedTextColor.RED)),
    SIMPLE_VOICE_CHAT(Component.translatable(I18nUtilities.get("log_type.simple_voice_chat"), NamedTextColor.AQUA)),
    DISCORD(Component.translatable(I18nUtilities.get("log_type.discord"), NamedTextColor.BLUE));

    public final Component component;

    LogType (final Component component) {
        this.component = component;
    }
}
