package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.I18nUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TrustedPlugin implements Listener {
    private final Bot bot;

    public final List<String> list;

    public TrustedPlugin (final Bot bot) {
        this.bot = bot;
        this.list = bot.config.trusted;

        bot.listener.addListener(this);
    }

    public void broadcast (final Component message, final UUID exceptTarget) {
        final Component component = Component.translatable(
                "[%s] [%s] %s",
                NamedTextColor.DARK_GRAY,
                Component.text("ChomeNS Bot", bot.colorPalette.primary),
                Component.text(this.bot.options.serverName, NamedTextColor.GRAY),
                message.colorIfAbsent(NamedTextColor.WHITE)
        );

        LoggerUtilities.log(LogType.TRUSTED_BROADCAST, component);

        for (final Bot bot : bot.bots) {
            if (bot == this.bot || !bot.loggedIn) continue;

            for (final String player : list) {
                final PlayerEntry entry = bot.players.getEntry(player);
                if (entry == null || entry.profile.getId() == exceptTarget) continue;
                bot.chat.tellraw(component, entry.profile.getId());
            }
        }
    }

    public void broadcast (final Component message) { broadcast(message, null); }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        if (!list.contains(target.profile.getName())) return;

        // based (VERY)
        final Component component;
        if (!target.profile.getName().equals(bot.config.ownerName)) {
            component = Component.translatable(
                    "Hello, %s!",
                    NamedTextColor.GREEN,
                    Component.text(target.profile.getName(), bot.colorPalette.username)
            );
        } else {
            final LocalDateTime now = LocalDateTime.now();

            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
            final String formattedTime = now.format(formatter);

            component = Component.translatable(
                    """
                            Hello, %s!
                            Time: %s
                            Online players: %s""",
                    NamedTextColor.GREEN,
                    Component.text(target.profile.getName(), bot.colorPalette.username),
                    Component.text(formattedTime, bot.colorPalette.string),
                    Component.text(bot.players.list.size(), bot.colorPalette.number)
            );
        }

        bot.chat.tellraw(
                component,
                target.profile.getId()
        );

        broadcast(
                Component.translatable(
                        I18nUtilities.get("trusted_broadcast.online"),
                        bot.colorPalette.defaultColor,
                        Component.text(target.profile.getName(), bot.colorPalette.username)
                ),
                target.profile.getId()
        );
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        if (!list.contains(target.profile.getName())) return;

        broadcast(
                Component.translatable(
                        I18nUtilities.get("trusted_broadcast.offline"),
                        bot.colorPalette.defaultColor,
                        Component.text(target.profile.getName(), bot.colorPalette.username)
                )
        );
    }
}
