package me.chayapak1.chomens_bot.commands;

import it.unimi.dsi.fastutil.Pair;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.chat.ChatPacketType;
import me.chayapak1.chomens_bot.data.player.PlayerEntry;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;

public class FindAltsCommand extends Command {
    // we allow both, since the flag used to be `allserver`
    private static final String ALL_SERVER_FLAG = "allserver";
    private static final String ALL_SERVERS_FLAG = "allservers";

    private static final int LIMIT = 200;

    public FindAltsCommand () {
        super(
                "findalts",
                new String[] { "-allservers <player|ip>", "<player|ip>" },
                new String[] { "alts", "sameip" },
                TrustLevel.PUBLIC,
                false,
                new ChatPacketType[] { ChatPacketType.DISGUISED }
        );
    }

    @Override
    public Component execute (final CommandContext context) throws Exception {
        final Bot bot = context.bot;

        if (Main.database == null)
            throw new CommandException(Component.translatable("commands.generic.error.database_disabled"));

        Main.database.checkOverloaded();

        final List<String> flags = context.getFlags(true, ALL_SERVER_FLAG, ALL_SERVERS_FLAG);

        final boolean allServer = flags.contains(ALL_SERVER_FLAG) || flags.contains(ALL_SERVERS_FLAG);

        final String player = context.getString(true, true);

        DatabasePlugin.EXECUTOR_SERVICE.execute(() -> {
            final PlayerEntry playerInTheServer = bot.players.getEntry(player);

            final String ipFromUsername;

            if (playerInTheServer == null || playerInTheServer.persistingData.ip == null) {
                ipFromUsername = bot.playersDatabase.getPlayerIP(player);
            } else {
                ipFromUsername = playerInTheServer.persistingData.ip;
            }

            if (ipFromUsername == null) {
                context.sendOutput(handle(bot, player, player, allServer));
            } else {
                context.sendOutput(handle(bot, ipFromUsername, player, allServer));
            }
        });

        return null;
    }

    private Component handle (final Bot bot, final String targetIP, final String player, final boolean allServer) {
        final Map<String, Pair<Long, String>> altsMap = bot.playersDatabase.findPlayerAlts(targetIP, allServer, LIMIT);

        final Component playerComponent = Component.text(player, bot.colorPalette.username);

        final boolean isIP = targetIP.equals(player);

        Component component = Component
                .translatable("commands.findalts.output", bot.colorPalette.defaultColor)
                .arguments(
                        Component.translatable(isIP ? "commands.findalts.ip" : "commands.findalts.player"),
                        isIP ?
                                playerComponent :
                                Component.translatable(
                                        "%s (%s)",
                                        playerComponent,
                                        Component.text(targetIP, bot.colorPalette.number)
                                )
                )
                .appendNewline();

        final List<String> sorted = altsMap.entrySet().stream()
                .sorted((a, b) -> {
                    final long aTime = a.getValue().left();
                    final long bTime = b.getValue().left();

                    return Long.compare(bTime, aTime);
                })
                .map(Map.Entry::getKey)
                .toList();

        int i = 0;
        for (final String username : sorted) {
            component = component
                    .append(
                            Component
                                    .text(username)
                                    .color((i++ & 1) == 0 ? bot.colorPalette.primary : bot.colorPalette.secondary)
                    )
                    .appendSpace();
        }

        return component;
    }
}
