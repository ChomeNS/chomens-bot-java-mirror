package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

// stolen from vencord :D
// https://github.com/Vendicated/Vencord/blob/1fa6181f7e6526e1bf2e85260d9051000916c47c/src/plugins/translate/utils.ts
public class TranslateCommand extends Command {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TRANSLATE_URL = "https://translate-pa.googleapis.com/v1/translate?" +
            "params.client=gtx&" +
            "dataTypes=TRANSLATION&" +
            "key=AIzaSyDLEeFI5OtFBwYBIoK_jj5m32rZK5CkCXA&" + // some google API key :eyes:
            "query.sourceLanguage=%s&" +
            "query.targetLanguage=%s&" +
            "query.text=%s";

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
                                TRANSLATE_URL,
                                URLEncoder.encode(from, StandardCharsets.UTF_8),
                                URLEncoder.encode(to, StandardCharsets.UTF_8),
                                URLEncoder.encode(message, StandardCharsets.UTF_8)
                        )
                ).toURL();

                final Result result = OBJECT_MAPPER.readValue(url, Result.class);

                context.sendOutput(
                        Component
                                .translatable(
                                        "Result: %s",
                                        Component.text(result.translation()).color(NamedTextColor.GREEN)
                                )
                                .color(bot.colorPalette.secondary)
                );
            } catch (final Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }

    /*
        $ curl "https://translate-pa.googleapis.com/v1/translate?params.client=gtx&dataTypes=TRANSLATION&key=AIzaSyDLEeFI5OtFBwYBIoK_jj5m32rZK5CkCXA&query.sourceLanguage=auto&query.targetLanguage=ja&query.text=hello"
        {
          "translation": "こんにちは",
          "detectedLanguages": {
            "srclangs": [
              "en"
            ],
            "extendedSrclangs": [
              "en"
            ]
          },
          "sourceLanguage": "en"
        }
     */
    private record Result(
            @JsonValue String translation,
            @JsonValue String sourceLanguage,
            @JsonValue DetectedLanguages detectedLanguages
    ) {
        private record DetectedLanguages(
                @JsonValue List<String> srclangs,
                @JsonValue List<String> extendedSrclangs
        ) { }
    }
}
