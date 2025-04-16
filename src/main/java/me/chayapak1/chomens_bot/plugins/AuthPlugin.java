package me.chayapak1.chomens_bot.plugins;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.listener.Listener;
import me.chayapak1.chomens_bot.data.logging.LogType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;

import java.util.concurrent.TimeUnit;

public class AuthPlugin implements Listener {
    private final Bot bot;

    public boolean isAuthenticating = false;
    public long startTime;

    public AuthPlugin (final Bot bot) {
        this.bot = bot;

        if (!bot.config.ownerAuthentication.enabled) return;

        bot.executor.scheduleAtFixedRate(() -> {
            if (!isAuthenticating || !bot.config.ownerAuthentication.enabled) return;

            checkAuthenticated();

            timeoutCheck();
        }, 500, 500, TimeUnit.MILLISECONDS);

        bot.listener.addListener(this);
    }

    private void checkAuthenticated () {
        final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

        if (target == null) return;

        if (!bot.chomeNSMod.connectedPlayers.contains(target)) return;

        isAuthenticating = false;

        bot.logger.log(
                LogType.AUTH,
                Component
                        .text("Player has been verified")
                        .color(NamedTextColor.GREEN)
        );

        bot.chat.tellraw(
                Component
                        .text("You have been verified")
                        .color(NamedTextColor.GREEN),
                target.profile.getId()
        );
    }

    private void timeoutCheck () {
        if (System.currentTimeMillis() - startTime < bot.config.ownerAuthentication.timeout) return;

        final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

        if (target == null) return;

        bot.filterManager.add(target, "Authentication timed out");
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName) || !bot.options.useCore) return;

        startTime = System.currentTimeMillis();
        isAuthenticating = true;
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName)) return;

        isAuthenticating = false;
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        isAuthenticating = false;
    }
}
