package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.Main;
import land.chipmunk.chayapak.chomens_bot.command.*;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

public class UrbanCommand extends Command {
    public int requestsPerSecond = 0;

    public UrbanCommand () {
        super(
                "urban",
                "Urban Dictionary in Minecraft",
                new String[] { "<term>" },
                new String[] {},
                TrustLevel.PUBLIC,
                false
        );

        Main.executor.scheduleAtFixedRate(() -> requestsPerSecond = 0, 0, 1, TimeUnit.SECONDS);
    }

    public Component execute (CommandContext context) throws CommandException {
        if (requestsPerSecond > 3) throw new CommandException(Component.text("Too many requests"));

        final Bot bot = context.bot;

        final boolean discord = context instanceof DiscordCommandContext;

        final String term = context.getString(true, true);

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
                int index = 1;
                for (JsonElement element : list) {
                    if (count >= 3) break;

                    final JsonObject definitionObject = element.getAsJsonObject();

                    final String word = definitionObject.get("word").getAsString();
                    final String _definition = definitionObject.get("definition").getAsString();

                    final DecimalFormat formatter = new DecimalFormat("#,###");

                    final String author = definitionObject.get("author").getAsString();
                    final String thumbsUp = formatter.format(definitionObject.get("thumbs_up").getAsInt());
                    final String thumbsDown = formatter.format(definitionObject.get("thumbs_down").getAsInt());
                    final String example = definitionObject.get("example").getAsString();

                    // whats the best way to implement this?
                    // also ohio code warning
                    Component definitionComponent = Component.empty();

                    final String definition = _definition.replaceAll("\r\n?", "\n");

                    final String[] splittedDefinition = definition.split("[\\[\\]]");
                    for (int i = 0; i < splittedDefinition.length; i++) {
                        final boolean even = i % 2 == 0;

                        final String wordWithDefinition = word + " - " + definition;

                        final Component globalHoverEvent = Component.translatable(
                                """
                                        Written by %s
                                        Thumbs up: %s
                                        Thumbs down: %s
                                        Example: %s""",
                                Component.text(author).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                                Component.text(thumbsUp).color(NamedTextColor.GREEN),
                                Component.text(thumbsDown).color(NamedTextColor.RED),
                                Component.text(example.replaceAll("\r\n?", "\n")).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                        );

                        if (even) {
                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splittedDefinition[i])
                                            .color(NamedTextColor.GRAY)
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            globalHoverEvent
                                                                    .append(Component.newline())
                                                                    .append(Component.text("Click here to copy the word and the definition to your clipboard").color(NamedTextColor.GREEN))
                                                    )
                                            )
                                            .clickEvent(ClickEvent.copyToClipboard(wordWithDefinition))
                            );
                        } else {
                            final String command = context.prefix +
                                    name +
                                    " " +
                                    splittedDefinition[i];

                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splittedDefinition[i])
                                            .style(Style.style(TextDecoration.UNDERLINED))
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            globalHoverEvent
                                                                    .append(Component.newline())
                                                                    .append(Component.text("Click here to run " + command).color(NamedTextColor.GREEN))
                                                    )
                                            )
                                            .clickEvent(
                                                    ClickEvent
                                                            .suggestCommand(command)
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
                                Component.text(index).color(NamedTextColor.GREEN),
                                Component.text(word).color(NamedTextColor.GRAY),
                                definitionComponent
                        ).color(NamedTextColor.DARK_GRAY);

                        context.sendOutput(component);
                    }

                    index++;
                }

                if (discord && !list.isEmpty()) context.sendOutput(discordComponent);
            } catch (Exception e) {
                e.printStackTrace();
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        requestsPerSecond++;

        return null;
    }
}
