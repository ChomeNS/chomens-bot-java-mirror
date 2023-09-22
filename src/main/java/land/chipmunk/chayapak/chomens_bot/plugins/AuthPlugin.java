package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.UUIDUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AuthPlugin extends PlayersPlugin.Listener {
    public final String id = "chomens_bot_verify"; // should this be static

    private final Bot bot;

    private String key = null;

    private long timeJoined;

    private boolean hasCorrectHash;

    private boolean started = false;

    public AuthPlugin (Bot bot) {
        this.bot = bot;

        if (!bot.config.ownerAuthentication.enabled) return;

        this.key = bot.config.ownerAuthentication.key;

        bot.players.addListener(this);
        bot.chat.addListener(new ChatPlugin.Listener() {
            @Override
            public void systemMessageReceived(Component component, boolean isCommandSuggestions, boolean isAuth, boolean isImposterFormat, String string, String ansi) {
                AuthPlugin.this.systemMessageReceived(component, isCommandSuggestions, isAuth, isImposterFormat);
            }
        });
        bot.executor.scheduleAtFixedRate(this::check, 0, 1, TimeUnit.SECONDS);
    }

    private String getSanitizedOwnerName() {
        return bot.config.ownerName.replaceAll("§[a-f0-9rlonmk]", "");
    }

    @Override
    public void playerJoined(PlayerEntry target) {
        if (!target.profile.getName().equals(getSanitizedOwnerName()) || !bot.options.useCore) return;

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

    private void systemMessageReceived (Component component, boolean isCommandSuggestions, boolean isAuth, boolean isImposterFormat) {
        try {
            if (isCommandSuggestions || !isAuth || !isImposterFormat) return;

            final List<Component> children = component.children();

            if (children.size() != 1) return;

            if (!(children.get(0) instanceof TextComponent)) return;

            final String inputHash = ((TextComponent) children.get(0)).content();

            final long time = System.currentTimeMillis() / 10_000;

            final String hash = Hashing.sha256()
                    // very pro hash input
                    .hashString(key + key + time + time, StandardCharsets.UTF_8)
                    .toString()
                    .substring(0, 8);

            hasCorrectHash = inputHash.equals(hash);
        } catch (Exception ignored) {}
    }

    private void check() {
        if (!started) return;

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
