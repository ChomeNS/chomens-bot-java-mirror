package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.LoggerUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class TrustedPlugin implements PlayersPlugin.Listener {
    private final Bot bot;

    public final List<String> list;

    public TrustedPlugin (Bot bot) {
        this.bot = bot;

        this.list = bot.config.trusted;

        bot.players.addListener(this);
    }

    public void broadcast (Component message, UUID exceptTarget) {
        final Component component = Component.translatable(
                "[%s] [%s] %s",
                Component.text("ChomeNS Bot").color(ColorUtilities.getColorByString(this.bot.config.colorPalette.primary)),
                Component.text(this.bot.options.serverName).color(NamedTextColor.GRAY),
                message.color(NamedTextColor.WHITE)
        ).color(NamedTextColor.DARK_GRAY);

        LoggerUtilities.log(LogType.TRUSTED_BROADCAST, component);

        for (Bot bot : bot.bots) {
            if (bot == this.bot || !bot.loggedIn) continue;

            for (String player : list) {
                final PlayerEntry entry = bot.players.getEntry(player);

                if (entry == null) continue;

                if (entry.profile.getId() == exceptTarget) continue;

                bot.chat.tellraw(component, player);
            }
        }
    }

    public void broadcast (Component message) { broadcast(message, null); }

    @Override
    public void playerJoined (PlayerEntry target) {
        if (!list.contains(target.profile.getName())) return;

        // based (VERY)
        Component component;
        if (!target.profile.getName().equals(bot.config.ownerName)) {
            component = Component.translatable(
                    "Hello, %s!",
                    Component.text(target.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
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
                    Component.text(target.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                    Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                    Component.text(bot.players.list.size()).color(ColorUtilities.getColorByString(bot.config.colorPalette.number))
            ).color(NamedTextColor.GREEN);
        }

        bot.chat.tellraw(
                component,
                target.profile.getId()
        );

        broadcast(
                Component.translatable(
                        "Trusted player %s is now online",
                        Component.text(target.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)),
                target.profile.getId()
        );
    }

    @Override
    public void playerLeft (PlayerEntry target) {
        if (!list.contains(target.profile.getName())) return;

        broadcast(
                Component.translatable(
                        "Trusted player %s is now offline",
                        Component.text(target.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
        );
    }
}
