package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.DiscordCommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class UrbanCommand extends Command {
    public int requestsPer500MS = 0;

    public UrbanCommand () {
        super(
                "urban",
                "Urban Dictionary in Minecraft",
                new String[] { "<{term}>" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );

        Main.executor.scheduleAtFixedRate(() -> requestsPer500MS = 0, 0, 500, TimeUnit.MILLISECONDS);
    }

    public Component execute (CommandContext context, String[] args, String[] fullArgs) {
        if (requestsPer500MS > 10) return Component.text("Too many requests").color(NamedTextColor.RED);

        final Bot bot = context.bot;

        final boolean discord = context instanceof DiscordCommandContext;

        final String term = String.join(" ", args);

        final Gson gson = new Gson();

        bot.executorService.submit(() -> {
            try {
                final URL url = new URL(
                        "https://api.urbandictionary.com/v0/define?term=" +
                                URLEncoder.encode(term, StandardCharsets.UTF_8)
                );

                final String jsonOutput = HttpUtilities.getRequest(url);

                final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

                final JsonArray list = jsonObject.getAsJsonArray("list");

                if (list.isEmpty()) context.sendOutput(Component.text("No results found").color(NamedTextColor.RED));

                Component discordComponent = Component.text("*Showing only 3 results because Discord*").append(Component.newline());

                int count = 0;
                for (JsonElement element : list) {
                    if (count >= 3) break;

                    final JsonObject definitionObject = element.getAsJsonObject();

                    final String word = definitionObject.get("word").getAsString();
                    final String _definition = definitionObject.get("definition").getAsString();

                    // whats the best way to implement this?
                    // also ohio code warning
                    Component definitionComponent = Component.empty();

                    final String[] splittedDefinition = _definition.replaceAll("\r\n?", "\n").split("[\\[\\]]");
                    for (int i = 0; i < splittedDefinition.length; i++) {
                        final boolean even = i % 2 == 0;

                        if (even) {
                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splittedDefinition[i])
                                            .color(NamedTextColor.GRAY)
                            );
                        } else {
                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splittedDefinition[i])
                                            .style(Style.style(TextDecoration.UNDERLINED))
                                            .clickEvent(
                                                    ClickEvent
                                                            .suggestCommand(
                                                                    context.prefix +
                                                                            name +
                                                                            " " +
                                                                            splittedDefinition[i]
                                                            )
                                            )
                                            .color(NamedTextColor.AQUA)
                            );
                        }
                    }

                    if (discord) {
                        discordComponent = discordComponent
                                .append(
                                    Component.translatable(
                                            "%s - %s",
                                            Component.text(word).color(NamedTextColor.GRAY),
                                            definitionComponent
                                    ).color(NamedTextColor.DARK_GRAY)
                                )
                                .append(Component.newline());

                        count++;
                    } else {
                        final Component component = Component.translatable(
                                "[%s] %s - %s",
                                Component.text("Urban").color(NamedTextColor.RED),
                                Component.text(word).color(NamedTextColor.GRAY),
                                definitionComponent
                        ).color(NamedTextColor.DARK_GRAY);

                        context.sendOutput(component);
                    }
                }

                if (discord && !list.isEmpty()) context.sendOutput(discordComponent);
            } catch (Exception e) {
                e.printStackTrace();
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        requestsPer500MS++;

        return null;
    }
}
