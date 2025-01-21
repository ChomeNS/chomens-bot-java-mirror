package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FindAltsCommand extends Command {
    public FindAltsCommand() {
        super(
                "findalts",
                "Finds players with the same IP address",
                new String[] { "-allserver <player|ip>", "<player|ip>" },
                new String[] { "alts", "sameip" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws Exception {
        final Bot bot = context.bot;

        if (bot.database == null) throw new CommandException(Component.text("Database is not enabled in the bot's config"));

        final String flag = context.getString(false, true);

        final boolean allServer = flag.equals("-allserver");

        String player = !allServer ? flag : ""; // adds the first argument if no flag

        player += context.getString(true, false);

        final PlayerEntry playerEntry = bot.players.getEntry(player);

        if (playerEntry == null) return handle(bot, player, true, player, allServer);
        else {
            final CompletableFuture<String> future = bot.players.getPlayerIP(playerEntry);

            if (future == null) return null;

            final String tempFinalPlayer = player;

            future.thenApplyAsync(targetIP -> {
                context.sendOutput(handle(bot, targetIP, false, tempFinalPlayer, allServer));

                return targetIP;
            });
        }

        return null;
    }

    private Component handle (Bot bot, String targetIP, boolean argumentIsIP, String player, boolean allServer) {
        final Map<String, JsonNode> altsMap = bot.playersDatabase.findPlayerAlts(targetIP, allServer);

        final Component playerComponent = Component
                .text(player)
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.username));

        Component component = Component
                .translatable("Possible alts for the %s %s:")
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                .arguments(
                        Component.text(argumentIsIP ? "IP" : "player"),
                        argumentIsIP ?
                                playerComponent :
                                Component.translatable("%s (%s)")
                                        .arguments(
                                                playerComponent,
                                                Component
                                                        .text(targetIP)
                                                        .color(ColorUtilities.getColorByString(bot.config.colorPalette.number))
                                        )
                )
                .appendNewline();

        final List<String> sorted = altsMap.entrySet().stream()
                .limit(200) // only find 200 alts because more than this is simply too many
                .sorted((a, b) -> {
                    final JsonNode aTimeNode = Optional.ofNullable(a.getValue().get("lastSeen"))
                            .map(node -> node.get("time"))
                            .orElse(null);
                    final JsonNode bTimeNode = Optional.ofNullable(b.getValue().get("lastSeen"))
                            .map(node -> node.get("time"))
                            .orElse(null);

                    if (aTimeNode == null && bTimeNode == null) return 0;
                    if (aTimeNode == null) return 1;
                    if (bTimeNode == null) return -1;

                    return Long.compare(bTimeNode.asLong(), aTimeNode.asLong());
                })
                .map(Map.Entry::getKey)
                .toList();

        int i = 0;
        for (String username : sorted) {
            component = component
                    .append(
                            Component
                                    .text(username)
                                    .color((i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .appendSpace();
        }

        return component;
    }
}
