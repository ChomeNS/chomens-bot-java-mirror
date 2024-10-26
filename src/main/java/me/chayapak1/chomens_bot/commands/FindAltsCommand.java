package me.chayapak1.chomens_bot.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.PlayersPersistentDataPlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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

        String targetIP;

        final String player = context.getString(true, true);

        final PlayerEntry playerEntry = bot.players.getEntry(player);

        if (playerEntry == null) targetIP = player;
        else {
            final CompletableFuture<String> future = bot.players.getPlayerIP(playerEntry);

            targetIP = future.get(5, TimeUnit.SECONDS);
        }

        if (targetIP == null) throw new CommandException(Component.text("Could not find player's IP address"));

        final Stream<String> matches = PlayersPersistentDataPlugin.playersObject.deepCopy().entrySet() // is calling deepCopy necessary?
                .stream()
                .filter(
                        entry -> {
                            final JsonObject ipsObject = entry
                                    .getValue().getAsJsonObject().getAsJsonObject("ips");

                            if (ipsObject == null) return false;

                            final JsonElement currentServerIP = ipsObject.get(bot.host + ":" + bot.port);

                            if (currentServerIP == null) return false;

                            return currentServerIP.getAsString().equals(targetIP);
                        }
                )
                .map(Map.Entry::getKey);

        Component component = Component
                .translatable("Possible alts for the player %s:")
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                .arguments(Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)))
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
