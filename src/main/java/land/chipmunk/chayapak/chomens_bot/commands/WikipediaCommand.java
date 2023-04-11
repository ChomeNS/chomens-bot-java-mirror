package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WikipediaCommand implements Command {
    public String name() { return "wikipedia"; }

    public String description() {
        return "Wikipedia in Minecraft";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{page}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("wiki");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute (CommandContext context, String[] args, String[] fullArgs) {
        final String page = String.join(" ", args);

        final Gson gson = new Gson();

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

            context.sendOutput(
                    Component.text(jsonObject.get("extract").getAsString()).color(NamedTextColor.GREEN)
            );
        } catch (FileNotFoundException ignored) {
            return Component.text("Cannot find page: " + page).color(NamedTextColor.RED);
        } catch (Exception e) {
            return Component.text(e.toString()).color(NamedTextColor.RED);
        }

        return Component.text("success");
    }
}
