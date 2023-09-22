package land.chipmunk.chayapak.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import land.chipmunk.chayapak.chomens_bot.Bot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FormatCheckerPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    public FormatCheckerPlugin (Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);
    }

    @Override
    public void systemMessageReceived(Component component, boolean isCommandSuggestions, boolean isAuth, boolean isImposterFormat, String string, String ansi) {
        if (!isImposterFormat) return;

        bot.chat.tellraw(Component.text("fake chomens custom chat .,.,.,.,"));
    }

    public boolean isImposterFormat (Component component) {
        if (!bot.config.imposterFormatChecker.enabled) return false;

        if (!(component instanceof TranslatableComponent format)) return false;

        final List<Component> args = format.args();
        if (args.size() < 3 || !format.key().equals("[%s] %s › %s")) return false;

        final Component nameComponent = format.args().get(1);

        if (!(nameComponent instanceof TextComponent)) return false;

        final String name = ((TextComponent) nameComponent).content();

        if (!name.equals(bot.config.ownerName)) return false;

        final Component prefix = format.args().get(0);

        if (!(prefix instanceof TranslatableComponent translatablePrefix)) return true;

        final Component userHash = translatablePrefix.args().get(0);

        if (userHash == null) return true;

        if (!(userHash instanceof TextComponent userHashComponent)) return true;

        final long time = System.currentTimeMillis() / 10_000;

        final String key = bot.config.imposterFormatChecker.key;

        final String hash = Hashing.sha256()
                // very pro hash input
                .hashString(key + key + time + time, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 8);

        return !hash.equals(userHashComponent.content());
    }
}
