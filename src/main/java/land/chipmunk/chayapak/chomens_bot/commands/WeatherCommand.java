package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WeatherCommand extends Command {
    public WeatherCommand () {
        super(
                "weather",
                "Shows the weather in a place",
                new String[] { "<{location}>" },
                new String[] {},
                TrustLevel.TRUSTED
        );
    }

    public Component execute (CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final String location = String.join(" ", args);

        final Gson gson = new Gson();

        try {
            final URL url = new URL(
                    "https://api.weatherapi.com/v1/current.json?key=" + bot.config().weatherApiKey() + "&q=" +
                            URLEncoder.encode(
                                location,
                                    StandardCharsets.UTF_8
                            )
                    + "&aqi=no"
            );

            final String jsonOutput = HttpUtilities.getRequest(url);

            final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

            final DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm:ss a");

            final DateTimeZone zone = DateTimeZone.forID(jsonObject.get("location").getAsJsonObject().get("tz_id").getAsString());

            final DateTime dateTime = new DateTime(zone);

            final String time = formatter.print(dateTime);

            return Component.translatable(
                    "Weather forecast for %s, %s:\n%s, feels like %s\nTime: %s",
                    Component.text(jsonObject.get("location").getAsJsonObject().get("name").getAsString()).color(ColorUtilities.getColorByString(bot.config().colorPalette().string())),
                    Component.text(jsonObject.get("location").getAsJsonObject().get("country").getAsString()).color(ColorUtilities.getColorByString(bot.config().colorPalette().string())),
                    Component.text(jsonObject.get("current").getAsJsonObject().get("temp_c").getAsString() + "°C").color(ColorUtilities.getColorByString(bot.config().colorPalette().secondary())),
                    Component.text(jsonObject.get("current").getAsJsonObject().get("feelslike_c").getAsString() + "°C").color(NamedTextColor.GREEN),
                    Component.text(time).color(ColorUtilities.getColorByString(bot.config().colorPalette().string()))
            ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
        } catch (Exception e) {
            return Component.text("Location \"" + location + "\" not found").color(NamedTextColor.RED);
        }
    }
}
