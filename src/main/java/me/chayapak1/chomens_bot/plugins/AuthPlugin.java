package me.chayapak1.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AuthPlugin extends PlayersPlugin.Listener {
    public final String id = "chomens_bot_verify"; // should this be static

    private final Bot bot;

    private String key = null;

    private long timeJoined;

    private boolean hasCorrectHash;

    private PlayerEntry targetPlayer;

    private boolean started = false;

    public AuthPlugin (Bot bot) {
        this.bot = bot;

        if (!bot.config.ownerAuthentication.enabled) return;

        this.key = bot.config.ownerAuthentication.key;

        bot.players.addListener(this);
        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public boolean systemMessageReceived(Component component, String string, String ansi) {
                return AuthPlugin.this.systemMessageReceived(component);
            }
        });
        bot.executor.scheduleAtFixedRate(this::check, 1, 3, TimeUnit.SECONDS);
    }

    private String getSanitizedOwnerName() {
        return bot.config.ownerName.replaceAll("§[a-f0-9rlonmk]", "");
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (!target.profile.getName().equals(getSanitizedOwnerName()) || !bot.options.useCore) return;

        targetPlayer = target;

        bot.executor.schedule(() -> sendVerificationMessage(target, true), 2, TimeUnit.SECONDS);
    }

    public void sendVerificationMessage (PlayerEntry entry, boolean setTimeJoined) {
        started = true;

        final long currentTime = System.currentTimeMillis();

        if (setTimeJoined) timeJoined = currentTime;

        final long time = currentTime / 10_000;

        final String hash = Hashing.sha256()
                .hashString(key + time, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 8);

        bot.chat.tellraw(
                Component
                        .text(id)
                        .append(Component.text(hash))
                        .append(Component.text(UUIDUtilities.selector(bot.profile.getId()))), // convenient reason
                entry.profile.getId()
        );
    }

    @Override
    public void playerLeft(PlayerEntry target) {
        if (!target.profile.getName().equals(getSanitizedOwnerName())) return;

        hasCorrectHash = false;
        started = false;
    }

    private boolean systemMessageReceived (Component component) {
        try {
            if (!(component instanceof TextComponent idComponent)) return true;

            if (!idComponent.content().equals(id)) return true;

            final List<Component> children = component.children();

            if (children.size() != 1) return true;

            if (!(children.get(0) instanceof TextComponent)) return true;

            final String inputHash = ((TextComponent) children.get(0)).content();

            final long time = System.currentTimeMillis() / 10_000;

            final String hash = Hashing.sha256()
                    // very pro hash input
                    .hashString(key + key + time + time, StandardCharsets.UTF_8)
                    .toString()
                    .substring(0, 8);

            bot.logger.custom(Component.text("Auth").color(NamedTextColor.RED), Component.text("Authenticating with real hash " + hash + " and user hash " + inputHash));

            hasCorrectHash = inputHash.equals(hash);

            if (hasCorrectHash && targetPlayer != null) bot.chat.tellraw(Component.text("You have been verified").color(NamedTextColor.GREEN), targetPlayer.profile.getId());

            return false;
        } catch (Exception ignored) {}

        return true;
    }

    private void check() {
        if (!started || !bot.config.ownerAuthentication.enabled) return;

        final PlayerEntry entry = bot.players.getEntry(getSanitizedOwnerName());

        if (entry == null) return;

        final long timeSinceJoined = System.currentTimeMillis() - timeJoined;

        if (!hasCorrectHash) sendVerificationMessage(entry, false);

        if (timeSinceJoined > bot.config.ownerAuthentication.timeout && !hasCorrectHash) {
            bot.filter.mute(entry, "Not verified");
            bot.filter.deOp(entry);
        }
    }
}
