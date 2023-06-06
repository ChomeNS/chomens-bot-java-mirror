package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class UrbanCommand implements Command {
    public String name() { return "urban"; }

    public String description() {
        return "Urban Dictionary in Minecraft";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{term}>");

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
        final String term = String.join(" ", args);

        final Gson gson = new Gson();

        new Thread(() -> {
            try {
                final URL url = new URL(
                        "https://api.urbandictionary.com/v0/define?term=" +
                                URLEncoder.encode(term, StandardCharsets.UTF_8)
                );

                final String jsonOutput = HttpUtilities.getRequest(url);

                final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

                final JsonArray list = jsonObject.getAsJsonArray("list");

                if (list.size() == 0) context.sendOutput(Component.text("No results found").color(NamedTextColor.RED));

                for (JsonElement element : list) {
                    final JsonObject definitionObject = element.getAsJsonObject();

                    final String word = definitionObject.get("word").getAsString();
                    final String definition = definitionObject.get("definition").getAsString();

                    final Component component = Component.translatable(
                            "[%s] %s - %s",
                            Component.text("Urban").color(NamedTextColor.RED),
                            Component.text(word).color(NamedTextColor.GRAY),
                            Component.text(definition).color(NamedTextColor.GRAY)
                    ).color(NamedTextColor.DARK_GRAY);

                    context.sendOutput(component);
                }
            } catch (Exception e) {
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        }).start();

        return null;
    }
}
