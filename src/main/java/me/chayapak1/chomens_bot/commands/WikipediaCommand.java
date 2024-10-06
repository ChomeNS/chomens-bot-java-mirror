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

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WikipediaCommand extends Command {
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
                final URL url = new URL(
                        "https://en.wikipedia.org/api/rest_v1/page/summary/" +
                                URLEncoder.encode(
                                        page.replace(" ", "_"), // mabe.
                                        StandardCharsets.UTF_8
                                )
                );

                final String jsonOutput = HttpUtilities.getRequest(url);

                final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

                context.sendOutput(Component.text(jsonObject.get("extract").getAsString()).color(NamedTextColor.GREEN));
            } catch (FileNotFoundException ignored) {
                context.sendOutput(Component.text("Cannot find page: " + page).color(NamedTextColor.RED));
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
