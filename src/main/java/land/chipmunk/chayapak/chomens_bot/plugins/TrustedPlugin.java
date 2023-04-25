package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.UUID;

public class TrustedPlugin extends PlayersPlugin.PlayerListener {
    private final Bot bot;

    @Getter private final List<String> list;

    public TrustedPlugin (Bot bot) {
        this.bot = bot;

        this.list = bot.config().trusted();

        bot.players().addListener(this);
    }

    public void broadcast (Component message, UUID exceptTarget) {
        for (Bot bot : bot.allBots()) {
            if (!bot.loggedIn()) continue;

            bot.logger().custom(Component.text("Trusted Broadcast").color(NamedTextColor.AQUA), message);

            for (String player : list) {
                final MutablePlayerListEntry entry = bot.players().getEntry(player);

                if (entry == null) continue;

                if (entry.profile().getId() == exceptTarget) continue;

                final Component component = Component.translatable(
                        "[%s] [%s] %s",
                        Component.text("ChomeNS Bot").color(NamedTextColor.YELLOW),
                        Component.text(this.bot.options().serverName()).color(NamedTextColor.GRAY),
                        message.color(NamedTextColor.WHITE)
                ).color(NamedTextColor.DARK_GRAY);

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
                        .append(Component.text(target.profile().getName()).color(NamedTextColor.GOLD))
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
                        Component.text(target.profile().getName()).color(NamedTextColor.GREEN)
                )
        );
    }
}
