package me.chayapak1.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WikipediaCommand extends Command {
    public static final String pageIDStringURL = "https://en.wikipedia.org/w/api.php?prop=info%%7Cpageprops&inprop=url&ppprop=disambiguation&titles=%s&format=json&redirects=&action=query&origin=*&";
    public static final String outputStringURL = "https://en.wikipedia.org/w/api.php?prop=extracts&explaintext=&exintro=&pageids=%d&format=json&redirects=&action=query&origin=*&";

    public WikipediaCommand () {
        super(
                "wikipedia",
                "Wikipedia in Minecraft",
                new String[] { "<page>" },
                new String[] { "wiki" },
                TrustLevel.PUBLIC,
                false
        );
    }

    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final String page = context.getString(true, true);

        final Gson gson = new Gson();

        bot.executorService.submit(() -> {
            try {
                Component component = Component.empty();

                final URL pageIDUrl = new URI(String.format(pageIDStringURL, URLEncoder.encode(page, StandardCharsets.UTF_8))).toURL();

                final JsonObject pageIDJsonOutput = gson.fromJson(HttpUtilities.getRequest(pageIDUrl), JsonObject.class);

                final JsonObject query = pageIDJsonOutput.getAsJsonObject("query");

                final JsonElement redirectsElement = query.get("redirects");
                if (redirectsElement != null) {
                    final JsonArray normalized = redirectsElement.getAsJsonArray();

                    for (JsonElement element : normalized) {
                        final JsonObject redirect = element.getAsJsonObject();

                        final String redirectedTo = redirect.get("to").getAsString();

                        component = component.append(
                                Component
                                        .translatable("Redirected to %s")
                                        .arguments(Component.text(redirectedTo))
                                        .style(
                                                Style.style()
                                                        .decorate(TextDecoration.ITALIC)
                                                        .color(NamedTextColor.GRAY)
                                        )
                        ).append(Component.newline());
                    }
                }

                final JsonObject pages = query.getAsJsonObject("pages");

                final int pageID = Integer.parseInt(pages.entrySet().iterator().next().getKey());

                if (pageID == -1) {
                    context.sendOutput(Component.text("Cannot find page: " + page).color(NamedTextColor.RED));
                    return;
                }

                final URL outputUrl = new URI(String.format(outputStringURL, pageID)).toURL();

                final JsonObject outputJsonOutput = gson.fromJson(HttpUtilities.getRequest(outputUrl), JsonObject.class);

                final JsonObject pageOutput = outputJsonOutput
                        .getAsJsonObject("query")
                        .getAsJsonObject("pages")
                        .getAsJsonObject(String.valueOf(pageID));

                final String title = pageOutput.get("title").getAsString();
                final String extract = pageOutput.get("extract").getAsString();

                component = component
                        .append(
                                Component
                                        .text(title)
                                        .style(
                                                Style.style()
                                                        .decorate(TextDecoration.BOLD)
                                                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                                        )
                        )
                        .append(Component.newline())
                        .append(Component.text(extract).color(NamedTextColor.GREEN));

                context.sendOutput(component);
            } catch (NumberFormatException e) {
                context.sendOutput(Component.text("Failed parsing page ID").color(NamedTextColor.RED));
                e.printStackTrace();
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
