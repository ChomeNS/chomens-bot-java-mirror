package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.PlayersPersistentDataPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class FindAltsCommand extends Command {
    public FindAltsCommand() {
        super(
                "findalts",
                "Finds players with the same IP address",
                new String[] { "<player>", "<ip>" },
                new String[] { "alts", "sameip" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws Exception {
        final Bot bot = context.bot;

        final String player = context.getString(true, true);

        final PlayerEntry playerEntry = bot.players.getEntry(player);

        if (playerEntry == null) return handle(bot, player, true, player);
        else {
            final CompletableFuture<String> future = bot.players.getPlayerIP(playerEntry);

            if (future == null) return null;

            future.thenApplyAsync(targetIP -> {
                context.sendOutput(handle(bot, targetIP, false, player));

                return targetIP;
            });
        }

        return null;
    }

    private Component handle (Bot bot, String targetIP, boolean argumentIsIP, String player) {
        PlayersPersistentDataPlugin.lock.lock();

        final Stream<String> matches = PlayersPersistentDataPlugin.playersObject.deepCopy().properties()
                .stream()
                .filter(
                        entry -> {
                            final ObjectNode ipsObject = (ObjectNode) entry
                                    .getValue().get("ips");

                            if (ipsObject == null || ipsObject.isNull()) return false;

                            final JsonNode currentServerIP = ipsObject.get(bot.host + ":" + bot.port);

                            if (currentServerIP == null || currentServerIP.isNull()) return false;

                            return currentServerIP.asText().equals(targetIP);
                        }
                )
                .map(Map.Entry::getKey);

        PlayersPersistentDataPlugin.lock.unlock();

        Component component = Component
                .translatable("Possible alts for the %s %s:")
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                .arguments(
                        Component.text(argumentIsIP ? "IP" : "player"),
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                )
                .appendNewline();

        int i = 0;
        for (String name : matches.toList()) {
            component = component
                    .append(
                            Component
                                    .text(name)
                                    .color((i++ & 1) == 0 ? ColorUtilities.getColorByString(bot.config.colorPalette.primary) : ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
                    )
                    .appendSpace();
        }

        return component;
    }
}
