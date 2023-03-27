package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.chatParsers.data.MutablePlayerListEntry;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class TrustedPlugin extends PlayersPlugin.PlayerListener {
    private final Bot bot;

    @Getter private final List<String> trusted;

    public TrustedPlugin (Bot bot) {
        this.bot = bot;

        this.trusted = bot.config().trusted();

        bot.players().addListener(this);
    }

    public void broadcast (Component message) {
        for (Bot allBot : bot.allBots()) {
            for (String player : trusted) {
                final Component component = Component.translatable(
                        "[%s] [%s] %s",
                        Component.text("ChomeNS Bot").color(NamedTextColor.YELLOW),
                        Component.text(bot.serverName()).color(NamedTextColor.GRAY),
                        message.color(NamedTextColor.WHITE)
                ).color(NamedTextColor.DARK_GRAY);

                allBot.chat().tellraw(component, player);
            }
        }
    }

    @Override
    public void playerJoined (MutablePlayerListEntry target) {
        if (!trusted.contains(target.profile().getName())) return;

        bot.chat().tellraw(
                Component.empty()
                        .append(Component.text("Hello, ").color(NamedTextColor.GREEN))
                        .append(Component.text(target.profile().getName()).color(NamedTextColor.GOLD))
                        .append(Component.text("!").color(NamedTextColor.GREEN))
        );

        broadcast(
                Component.translatable(
                        "Trusted player %s is now online",
                        Component.text(target.profile().getName()).color(NamedTextColor.GREEN)
                )
        );
    }

    @Override
    public void playerLeft (MutablePlayerListEntry target) {
        if (!trusted.contains(target.profile().getName())) return;

        broadcast(
                Component.translatable(
                        "Trusted player %s is now offline",
                        Component.text(target.profile().getName()).color(NamedTextColor.GREEN)
                )
        );
    }
}
