package me.chayapak1.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WeatherCommand extends Command {
    public WeatherCommand () {
        super(
                "weather",
                "Shows the weather in a place",
                new String[] { "<location>" },
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    public Component execute (final CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String location = context.getString(true, true);

        final Gson gson = new Gson();

        try {
            final URL url = new URI(
                    "https://api.weatherapi.com/v1/current.json?key=" + bot.config.weatherApiKey + "&q=" +
                            URLEncoder.encode(
                                    location,
                                    StandardCharsets.UTF_8
                            )
            ).toURL();

            final String jsonOutput = HttpUtilities.getRequest(url);

            final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

            return Component.translatable(
                    "Current weather for %s, %s:\n%s (%s), feels like %s (%s)\nCondition: %s\nCloud cover: %s\nHumidity: %s\nTime: %s",
                    Component.text(jsonObject.get("location").getAsJsonObject().get("name").getAsString()).color(bot.colorPalette.string),
                    Component.text(jsonObject.get("location").getAsJsonObject().get("country").getAsString()).color(bot.colorPalette.string),
                    Component
                            .empty()
                            .append(
                                    Component
                                            .text(
                                                    jsonObject
                                                            .get("current")
                                                            .getAsJsonObject()
                                                            .get("temp_c")
                                                            .getAsString() + "°C"
                                            )
                                            .color(bot.colorPalette.secondary)
                            ),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("temp_f")
                                            .getAsString() + "°F"
                            )
                            .color(bot.colorPalette.secondary),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("feelslike_c")
                                            .getAsString() + "°C"
                            )
                            .color(NamedTextColor.GREEN),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("feelslike_f")
                                            .getAsString() + "°F"
                            )
                            .color(NamedTextColor.GREEN),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("condition")
                                            .getAsJsonObject()
                                            .get("text")
                                            .getAsString()
                            )
                            .color(bot.colorPalette.string),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("cloud")
                                            .getAsInt()
                            )
                            .append(Component.text("%"))
                            .color(bot.colorPalette.number),
                    Component
                            .text(
                                    jsonObject
                                            .get("current")
                                            .getAsJsonObject()
                                            .get("humidity")
                                            .getAsInt()
                            )
                            .append(Component.text("%"))
                            .color(bot.colorPalette.number),
                    Component
                            .text(
                                    jsonObject
                                            .get("location")
                                            .getAsJsonObject()
                                            .get("localtime")
                                            .getAsString()
                            )
                            .color(bot.colorPalette.string)
            ).color(bot.colorPalette.defaultColor);
        } catch (final Exception e) {
            throw new CommandException(Component.text("Location \"" + location + "\" not found"));
        }
    }
}
