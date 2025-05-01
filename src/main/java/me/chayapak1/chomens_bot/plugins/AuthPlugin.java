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
    public int seconds = 0;

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

        cleanup();

        bot.logger.log(
                LogType.AUTH,
                Component
                        .text("Player has been verified")
                        .color(NamedTextColor.GREEN)
        );

        bot.chomeNSMod.sendMessage(
                target,
                Component
                        .text("You have been verified")
                        .color(NamedTextColor.GREEN)
        );
    }

    private void timeoutCheck () {
        if (seconds < bot.config.ownerAuthentication.timeout) return;

        final PlayerEntry target = bot.players.getEntry(bot.config.ownerName);

        if (target == null) return;

        bot.filterManager.add(target, "Authentication timed out");
    }

    private void cleanup () {
        isAuthenticating = false;
        seconds = 0;
    }

    @Override
    public void onSecondTick () {
        if (isAuthenticating) seconds++;
    }

    @Override
    public void onPlayerJoined (final PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName) || !bot.options.useCore) return;

        isAuthenticating = true;
    }

    @Override
    public void onPlayerLeft (final PlayerEntry target) {
        if (!target.profile.getName().equals(bot.config.ownerName)) return;

        cleanup();
    }

    @Override
    public void disconnected (final DisconnectedEvent event) {
        cleanup();
    }
}
