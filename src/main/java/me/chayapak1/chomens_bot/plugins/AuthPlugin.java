package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AuthPlugin extends PlayersPlugin.Listener {
    private final Bot bot;

    private final String ownerIpForServer;

    public AuthPlugin (Bot bot) {
        this.bot = bot;

        this.ownerIpForServer = bot.config.ownerAuthentication.ips.get(bot.getServerString());

        if (!bot.config.ownerAuthentication.enabled || ownerIpForServer == null) return;

        bot.players.addListener(this);
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName) || !bot.options.useCore) return;

        final CompletableFuture<String> future = bot.players.getPlayerIP(target, true);

        future.completeOnTimeout("", 10, TimeUnit.SECONDS);

        future.thenApplyAsync(ip -> {
            bot.logger.custom(
                    Component
                            .text("Auth")
                            .color(NamedTextColor.RED),
                    Component.translatable(
                            "Authenticating with user IP %s and configured owner IP %s",
                            Component.text(ip),
                            Component.text(ownerIpForServer)
                    )
            );

            if (ip.equals(ownerIpForServer)) {
                bot.chat.tellraw(
                        Component
                                .text("You have been verified")
                                .color(NamedTextColor.GREEN),
                        target.profile.getId()
                );
            } else {
                bot.filterManager.doAll(target, bot.config.ownerAuthentication.muteReason);
            }

            return ip;
        });
    }
}
