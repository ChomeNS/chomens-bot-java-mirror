package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Optional;

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

            if (playerInTheServer == null || playerInTheServer.persistingData.ip == null)
                ipFromUsername = bot.playersDatabase.getPlayerIP(player);
            else ipFromUsername = playerInTheServer.persistingData.ip;

            if (ipFromUsername == null) {
                context.sendOutput(handle(bot, player, player, allServer));
            } else {
                context.sendOutput(handle(bot, ipFromUsername, player, allServer));
            }
        });

        return null;
    }

    private Component handle (final Bot bot, final String targetIP, final String player, final boolean allServer) {
        final Map<String, JsonNode> altsMap = bot.playersDatabase.findPlayerAlts(targetIP, allServer, LIMIT);

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
                                        Component
                                                .text(targetIP)
                                                .color(bot.colorPalette.number)
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
