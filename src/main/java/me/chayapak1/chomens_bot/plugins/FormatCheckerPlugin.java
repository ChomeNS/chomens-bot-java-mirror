package me.chayapak1.chomens_bot.plugins;

import com.google.common.hash.Hashing;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class FormatCheckerPlugin implements ChatPlugin.Listener, PlayersPlugin.Listener {
    private final Bot bot;

    private int totalFormat = 0;

    public FormatCheckerPlugin (final Bot bot) {
        this.bot = bot;

        bot.chat.addListener(this);
        bot.players.addListener(this);
    }

    private void reset (final PlayerEntry entry) {
        if (!entry.profile.getName().equals(bot.config.ownerName)) return;

        totalFormat = 0;
    }

    @Override
    public void playerJoined (final PlayerEntry target) {
        reset(target);
    }

    @Override
    public void playerLeft (final PlayerEntry target) {
        reset(target);
    }

    @Override
    public boolean systemMessageReceived (final Component component, final String string, final String ansi) {
        if (!isImposterFormat(component)) return true;

        bot.chat.tellraw(Component.text("Possible fake ChomeNS custom chat").style(Style.style(TextDecoration.ITALIC)).color(NamedTextColor.GRAY));

        return true;
    }

    public boolean isImposterFormat (final Component component) {
        if (!bot.config.imposterFormatChecker.enabled) return false;

        if (!(component instanceof final TranslatableComponent format)) return false;

        final List<TranslationArgument> args = format.arguments();
        if (args.size() < 3 || !format.key().equals("[%s] %s › %s")) return false;

        final Object nameComponent = format.arguments().get(1).value();

        if (!(nameComponent instanceof TextComponent)) return false;

        final String name = ((TextComponent) nameComponent).content();

        if (!name.equals(bot.config.ownerName)) return false;

        final Object prefix = format.arguments().getFirst().value();

        if (
                ((prefix instanceof final TextComponent text) && text.content().equals(bot.username + " Console")) || // ohio
                        (Main.discord != null && prefix.equals(Main.discord.messagePrefix))
        ) return false;

        if (!(prefix instanceof final TranslatableComponent translatablePrefix)) return true;

        final Object userHash = translatablePrefix.arguments().getFirst().value();

        if (!(userHash instanceof final TextComponent userHashComponent)) return true;

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
