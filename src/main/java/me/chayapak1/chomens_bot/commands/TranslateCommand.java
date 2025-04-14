package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.HTMLUtilities;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslateCommand extends Command {
    private static final String TRANSLATE_MOBILE_URL = "https://translate.google.com/m?sl=%s&tl=%s&hl=en&q=%s";
    private static final Pattern FIND_RESULT_PATTERN = Pattern.compile(
            "class=\"result-container\">([^<]*)</div>",
            Pattern.MULTILINE
    );

    public TranslateCommand () {
        super(
                "translate",
                "Translates a message using Google Translate",
                new String[] { "<from> <to> <message>" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String from = context.getString(false, true);
        final String to = context.getString(false, true);

        final String message = context.getString(true, true);

        bot.executorService.submit(() -> {
            try {
                final URL url = new URI(
                        String.format(
                                TRANSLATE_MOBILE_URL,
                                URLEncoder.encode(from, StandardCharsets.UTF_8),
                                URLEncoder.encode(to, StandardCharsets.UTF_8),
                                URLEncoder.encode(message, StandardCharsets.UTF_8)
                        )
                ).toURL();

                final String html = HttpUtilities.getRequest(url);

                final Matcher matcher = FIND_RESULT_PATTERN.matcher(html);

                if (!matcher.find()) return;

                final String match = matcher.group(1);
                if (match == null) return;

                final String result = HTMLUtilities.toFormattingCodes(match);
                if (result == null) return;

                context.sendOutput(
                        Component
                                .translatable(
                                        "Result: %s",
                                        Component.text(result).color(NamedTextColor.GREEN)
                                )
                                .color(bot.colorPalette.secondary)
                );
            } catch (final Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
