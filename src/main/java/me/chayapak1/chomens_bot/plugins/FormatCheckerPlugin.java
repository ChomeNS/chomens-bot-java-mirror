package me.chayapak1.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FormatCheckerPlugin extends ChatPlugin.Listener {
    private final Bot bot;

    private int totalFormat = 0;

    public FormatCheckerPlugin (Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);

        bot.players.addListener(new PlayersPlugin.Listener() {
            @Override
            public void playerJoined(PlayerEntry target) {
                reset(target);
            }

            @Override
            public void playerLeft(PlayerEntry target) {
                reset(target);
            }
        });
    }

    private void reset (PlayerEntry entry) {
        if (!entry.profile.getName().equals(bot.config.ownerName)) return;

        totalFormat = 0;
    }

    @Override
    public boolean systemMessageReceived(Component component, String string, String ansi) {
        if (!isImposterFormat(component)) return true;

        bot.chat.tellraw(Component.text("Possible fake ChomeNS custom chat").style(Style.style(TextDecoration.ITALIC)).color(NamedTextColor.GRAY));

        return true;
    }

    public boolean isImposterFormat (Component component) {
        if (!bot.config.imposterFormatChecker.enabled) return false;

        if (!(component instanceof TranslatableComponent format)) return false;

        final List<TranslationArgument> args = format.arguments();
        if (args.size() < 3 || !format.key().equals("[%s] %s › %s")) return false;

        final TranslationArgument nameComponent = format.arguments().get(1);

        if (!(nameComponent instanceof TextComponent)) return false;

        final String name = ((TextComponent) nameComponent).content();

        if (!name.equals(bot.config.ownerName)) return false;

        final Component prefix = (Component) format.arguments().get(0);

        if (
                ((prefix instanceof TextComponent text) && text.content().equals(bot.username + " Console")) || // ohio
                        (bot.discord != null && prefix.equals(bot.discord.messagePrefix))
        ) return false;

        if (!(prefix instanceof TranslatableComponent translatablePrefix)) return true;

        final TranslationArgument userHash = translatablePrefix.arguments().get(0);

        if (userHash == null) return true;

        if (!(userHash instanceof TextComponent userHashComponent)) return true;

        final String key = bot.config.imposterFormatChecker.key;

        final String hash = Hashing.sha256()
                // very pro hash input
                .hashString(key + totalFormat, StandardCharsets.UTF_8)
                .toString()
                .substring(0, 8);

        final boolean correct = hash.equals(userHashComponent.content());

        if (correct) totalFormat++;

        return !correct;
    }
}
