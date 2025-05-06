package me.chayapak1.chomens_bot.commands;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.command.contexts.DiscordCommandContext;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.util.HttpUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UrbanCommand extends Command {
    public final AtomicInteger requestsPerSecond = new AtomicInteger();

    public UrbanCommand () {
        super(
                "urban",
                new String[] { "<term>" },
                new String[] {},
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[]{ ChatPacketType.DISGUISED }
        );

        Main.EXECUTOR.scheduleAtFixedRate(() -> requestsPerSecond.set(0), 0, 1, TimeUnit.SECONDS);
    }

    public Component execute (final CommandContext context) throws CommandException {
        if (requestsPerSecond.get() > 3) throw new CommandException(Component.translatable("commands.urban.error.too_many_requests"));

        final Bot bot = context.bot;

        final boolean discord = context instanceof DiscordCommandContext;

        final String term = context.getString(true, true);

        final Gson gson = new Gson();

        bot.executorService.submit(() -> {
            try {
                final URL url = new URI(
                        "https://api.urbandictionary.com/v0/define?term=" +
                                URLEncoder.encode(term, StandardCharsets.UTF_8)
                ).toURL();

                final String jsonOutput = HttpUtilities.getRequest(url);

                final JsonObject jsonObject = gson.fromJson(jsonOutput, JsonObject.class);

                final JsonArray list = jsonObject.getAsJsonArray("list");

                if (list.isEmpty()) context.sendOutput(Component.translatable("commands.urban.error.no_results", NamedTextColor.RED));

                Component discordComponent = Component.translatable("commands.urban.discord_warning").append(Component.newline());

                int count = 0;
                int index = 1;
                for (final JsonElement element : list) {
                    if (count >= 3) break;

                    final JsonObject definitionObject = element.getAsJsonObject();

                    final String word = definitionObject.get("word").getAsString();
                    final String originalDefinition = definitionObject.get("definition").getAsString();

                    final DecimalFormat formatter = new DecimalFormat("#,###");

                    final String author = definitionObject.get("author").getAsString();
                    final String thumbsUp = formatter.format(definitionObject.get("thumbs_up").getAsInt());
                    final String thumbsDown = formatter.format(definitionObject.get("thumbs_down").getAsInt());
                    final String example = definitionObject.get("example").getAsString();

                    // what's the best way to implement this?
                    // also ohio code warning
                    Component definitionComponent = Component.empty();

                    final String definition = originalDefinition.replaceAll("\r\n?", "\n");

                    final String[] splitDefinition = definition.split("[\\[\\]]");
                    for (int i = 0; i < splitDefinition.length; i++) {
                        final boolean even = i % 2 == 0;

                        final String wordWithDefinition = word + " - " + definition;

                        final Component globalHoverEvent = Component.translatable(
                                "commands.urban.hover.info",
                                Component.text(author, bot.colorPalette.string),
                                Component.text(thumbsUp, NamedTextColor.GREEN),
                                Component.text(thumbsDown, NamedTextColor.RED),
                                Component.text(example.replaceAll("\r\n?", "\n"), bot.colorPalette.string)
                        );

                        if (even) {
                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splitDefinition[i], NamedTextColor.GRAY)
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            globalHoverEvent
                                                                    .append(Component.newline())
                                                                    .append(Component.translatable("commands.urban.hover.copy", NamedTextColor.GREEN))
                                                    )
                                            )
                                            .clickEvent(ClickEvent.copyToClipboard(wordWithDefinition))
                            );
                        } else {
                            final String command = context.prefix +
                                    name +
                                    " " +
                                    splitDefinition[i];

                            definitionComponent = definitionComponent.append(
                                    Component
                                            .text(splitDefinition[i])
                                            .style(Style.style(TextDecoration.UNDERLINED))
                                            .hoverEvent(
                                                    HoverEvent.showText(
                                                            globalHoverEvent
                                                                    .append(Component.newline())
                                                                    .append(
                                                                            Component.translatable(
                                                                                    "commands.urban.hover.run",
                                                                                    NamedTextColor.GREEN,
                                                                                    Component.text(command)
                                                                            )
                                                                    )
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
            } catch (final Exception e) {
                bot.logger.error(e);
                context.sendOutput(Component.text(e.toString()).color(NamedTextColor.RED));
            }
        });

        requestsPerSecond.getAndIncrement();

        return null;
    }
}
