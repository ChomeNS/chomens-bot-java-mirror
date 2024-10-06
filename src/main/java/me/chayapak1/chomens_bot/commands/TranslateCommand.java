package me.chayapak1.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TranslateCommand extends Command {
    public TranslateCommand () {
        super(
                "translate",
                "Translates a message using Google Translate",
                new String[] { "<fromLanguage> <toLanguage> <message>" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String from = context.getString(false, true);
        final String to = context.getString(false, true);

        final String message = context.getString(true, true);

        final Gson gson = new Gson();

        bot.executorService.submit(() -> {
            try {
                final URL url = new URL("https://translate.google.com/translate_a/single?client=at&dt=t&dt=rm&dj=1");

                final String jsonOutput = HttpUtilities.postRequest(
                        url,
                        "application/x-www-form-urlencoded;charset=utf-8",
                        String.format(
                                "sl=%s&tl=%s&q=%s",
                                from,
                                to,
                                URLEncoder.encode(
                                        message,
                                        StandardCharsets.UTF_8
                                )
                        )
                );

                final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

                final JsonArray sentences = jsonObject.getAsJsonArray("sentences");

                final JsonObject translation = sentences.get(0).getAsJsonObject();

                final String output = translation.get("trans").getAsString();

                context.sendOutput(
                        Component
                            .translatable(
                                    "Result: %s",
                                    Component.text(output).color(NamedTextColor.GREEN)
                            )
                            .color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                );
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
