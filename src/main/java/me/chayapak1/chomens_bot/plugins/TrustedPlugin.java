package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
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
                Component.text("ChomeNS Bot").color(bot.colorPalette.primary),
                Component.text(this.bot.options.serverName).color(NamedTextColor.GRAY),
                message.color(NamedTextColor.WHITE)
        ).color(NamedTextColor.DARK_GRAY);

        LoggerUtilities.log(LogType.TRUSTED_BROADCAST, component);

        for (final Bot bot : bot.bots) {
            if (bot == this.bot || !bot.loggedIn) continue;

            for (final String player : list) {
                final PlayerEntry entry = bot.players.getEntry(player);

                if (entry == null) continue;

                if (entry.profile.getId() == exceptTarget) continue;

                bot.chat.tellraw(component, player);
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
                    Component.text(target.profile.getName()).color(bot.colorPalette.username)
            ).color(NamedTextColor.GREEN);
        } else {
            final LocalDateTime now = LocalDateTime.now();

            final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
            final String formattedTime = now.format(formatter);

            component = Component.translatable(
                    """
                            Hello, %s!
                            Time: %s
                            Online players: %s""",
                    Component.text(target.profile.getName()).color(bot.colorPalette.username),
                    Component.text(formattedTime).color(bot.colorPalette.string),
                    Component.text(bot.players.list.size()).color(bot.colorPalette.number)
            ).color(NamedTextColor.GREEN);
        }

        bot.chat.tellraw(
                component,
                target.profile.getId()
        );

        broadcast(
                Component.translatable(
                        "Trusted player %s is now online",
                        Component.text(target.profile.getName()).color(bot.colorPalette.username)
                ).color(bot.colorPalette.defaultColor),
                target.profile.getId()
        );
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        if (!list.contains(target.profile.getName())) return;

        broadcast(
                Component.translatable(
                        "Trusted player %s is now offline",
                        Component.text(target.profile.getName()).color(bot.colorPalette.username)
                ).color(bot.colorPalette.defaultColor)
        );
    }
}
