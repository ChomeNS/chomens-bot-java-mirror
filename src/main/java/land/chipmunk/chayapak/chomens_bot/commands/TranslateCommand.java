package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TranslateCommand extends Command {
    public TranslateCommand () {
        super(
                "translate",
                "Translates a message using Google Translate",
                new String[] { "<fromLanguage> <toLanguage> <{message}>" },
                new String[] {},
                TrustLevel.PUBLIC,
false
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        final String from = args[0];
        final String to = args[1];

        final String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

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
