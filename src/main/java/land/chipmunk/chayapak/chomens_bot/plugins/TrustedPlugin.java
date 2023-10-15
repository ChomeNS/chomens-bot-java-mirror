package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;
import java.util.UUID;

public class TrustedPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    public final List<String> list;

    public TrustedPlugin (Bot bot) {
        this.bot = bot;

        this.list = bot.config.trusted;

        bot.players.addListener(this);
    }

    public void broadcast (Component message, UUID exceptTarget) {
        for (Bot bot : bot.bots) {
            if (!bot.loggedIn) continue;

            final Component component = Component.translatable(
                    "[%s] [%s] %s",
                    Component.text("ChomeNS Bot").color(ColorUtilities.getColorByString(bot.config.colorPalette.primary)),
                    Component.text(this.bot.options.serverName).color(NamedTextColor.GRAY),
                    message.color(NamedTextColor.WHITE)
            ).color(NamedTextColor.DARK_GRAY);

            bot.logger.custom(Component.text("Trusted Broadcast").color(NamedTextColor.AQUA), component);

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
            final DateTime now = DateTime.now();

            final DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm:ss a");
            final String formattedTime = formatter.print(now);

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
