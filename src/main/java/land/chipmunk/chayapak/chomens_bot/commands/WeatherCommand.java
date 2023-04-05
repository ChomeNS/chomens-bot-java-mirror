package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WeatherCommand implements Command {
    public String name() { return "weather"; }

    public String description() {
        return "Shows the weather in a place";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{location}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 0;
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

            final String jsonOutput = HttpUtilities.request(url);

            final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

            final DateTimeFormatter formatter = DateTimeFormat.forPattern("hh:mm:ss a");

            final DateTimeZone zone = DateTimeZone.forID(jsonObject.get("location").getAsJsonObject().get("tz_id").getAsString());

            final DateTime dateTime = new DateTime(zone);

            final String time = formatter.print(dateTime);

            final Component component = Component.translatable(
                    "Weather for %s, %s:\n%s, feels like %s\nTime: %s",
                    Component.text(jsonObject.get("location").getAsJsonObject().get("name").getAsString()).color(NamedTextColor.AQUA),
                    Component.text(jsonObject.get("location").getAsJsonObject().get("country").getAsString()).color(NamedTextColor.AQUA),
                    Component.text(jsonObject.get("current").getAsJsonObject().get("temp_c").getAsString() + "°C").color(NamedTextColor.GOLD),
                    Component.text(jsonObject.get("current").getAsJsonObject().get("feelslike_c").getAsString() + "°C").color(NamedTextColor.GREEN),
                    Component.text(time).color(NamedTextColor.AQUA)

            );

            context.sendOutput(component);
        } catch (Exception e) {
            return Component.text("Location \"" + location + "\" not found").color(NamedTextColor.RED);
        }

        return Component.text("success");
    }
}
