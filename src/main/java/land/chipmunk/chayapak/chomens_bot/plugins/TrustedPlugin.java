package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;

public class TrustedPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    @Getter private final List<String> list;

    public TrustedPlugin (Bot bot) {
        this.bot = bot;

        this.list = bot.config().trusted();

        bot.players().addListener(this);
    }

    public void broadcast (Component message, UUID exceptTarget) {
        for (Bot bot : bot.bots()) {
            if (!bot.loggedIn()) continue;

            final Component component = Component.translatable(
                    "[%s] [%s] %s",
                    Component.text("ChomeNS Bot").color(ColorUtilities.getColorByString(bot.config().colorPalette().primary())),
                    Component.text(this.bot.options().serverName()).color(NamedTextColor.GRAY),
                    message.color(NamedTextColor.WHITE)
            ).color(NamedTextColor.DARK_GRAY);

            bot.logger().custom(Component.text("Trusted Broadcast").color(NamedTextColor.AQUA), component);

            for (String player : list) {
                final MutablePlayerListEntry entry = bot.players().getEntry(player);

                if (entry == null) continue;

                if (entry.profile().getId() == exceptTarget) continue;

                bot.chat().tellraw(component, player);
            }
        }
    }

    public void broadcast (Component message) { broadcast(message, null); }

    // these can be used in servereval
    public void broadcast (String message) { broadcast(Component.text(message), null); }
    public void broadcast (String message, UUID exceptTarget) { broadcast(Component.text(message), exceptTarget); }

    @Override
    public void playerJoined (MutablePlayerListEntry target) {
        if (!list.contains(target.profile().getName())) return;

        bot.chat().tellraw(
                Component.empty()
                        .append(Component.text("Hello, ").color(NamedTextColor.GREEN))
                        .append(Component.text(target.profile().getName()).color(ColorUtilities.getColorByString(bot.config().colorPalette().username())))
                        .append(Component.text("!").color(NamedTextColor.GREEN)),
                target.profile().getId()
        );

        broadcast(
                Component.translatable(
                        "Trusted player %s is now online",
                        Component.text(target.profile().getName()).color(NamedTextColor.GREEN)
                ),
                target.profile().getId()
        );
    }

    @Override
    public void playerLeft (MutablePlayerListEntry target) {
        if (!list.contains(target.profile().getName())) return;

        broadcast(
                Component.translatable(
                        "Trusted player %s is now offline",
                        Component.text(target.profile().getName()).color(ColorUtilities.getColorByString(bot.config().colorPalette().username()))
                ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()))
        );
    }
}
