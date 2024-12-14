package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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

        if (bot.database == null) throw new CommandException(Component.text("Database is not enabled in the bot's config"));

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
        final List<String> alts = bot.playersDatabase.findPlayerAlts(targetIP);

        Component component = Component
                .translatable("Possible alts for the %s %s:")
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor))
                .arguments(
                        Component.text(argumentIsIP ? "IP" : "player"),
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username))
                )
                .appendNewline();

        int i = 0;
        for (String username : alts) {
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
