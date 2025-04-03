package me.chayapak1.chomens_bot.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.Main;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.plugins.DatabasePlugin;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SeenCommand extends Command {
    public SeenCommand () {
        super(
                "seen",
                "Shows the last seen of a player",
                new String[] { "<player>" },
                new String[] { "lastseen" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute (CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        if (Main.database == null)
            throw new CommandException(Component.text("Database is not enabled in the bot's config"));

        Main.database.checkOverloaded();

        final String player = context.getString(true, true);

        boolean online = false;

        final List<Component> onlineComponents = new ArrayList<>();

        for (Bot eachBot : bot.bots) {
            if (eachBot.players.getEntry(player) != null) {
                online = true;

                onlineComponents.add(
                        Component.empty()
                                .append(Component.text(player))
                                .append(Component.text(" is currently online on "))
                                .append(Component.text(eachBot.getServerString()))
                                .color(NamedTextColor.RED)
                );
            }
        }

        if (online) return Component.join(JoinConfiguration.newlines(), onlineComponents);

        DatabasePlugin.EXECUTOR_SERVICE.submit(() -> {
            try {
                final JsonNode playerElement = bot.playersDatabase.getPlayerData(player);
                if (playerElement == null) throw new CommandException(Component.translatable(
                        "%s was never seen",
                        Component.text(player)
                ));

                final ObjectNode lastSeen = (ObjectNode) playerElement.get("lastSeen");

                if (lastSeen == null || lastSeen.isNull())
                    throw new CommandException(Component.text("This player doesn't seem to have the last seen entry in the database for some reason."));

                final JsonNode time = lastSeen.get("time");

                if (time == null || time.isNull())
                    throw new CommandException(Component.text("This player doesn't seem to have the `lastSeen.time` entry in the database for some reason."));

                final Instant instant = Instant.ofEpochMilli(time.asLong());
                final ZoneId zoneId = ZoneId.of("UTC"); // should i be doing this?
                final OffsetDateTime localDateTime = OffsetDateTime.ofInstant(instant, zoneId);

                final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy, hh:mm:ss a Z");
                final String formattedTime = localDateTime.format(formatter);

                final String server = lastSeen.get("server").asText();

                context.sendOutput(Component.translatable(
                        "%s was last seen at %s on %s",
                        Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                        Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                        Component.text(server).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
                ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)));
            } catch (CommandException e) {
                context.sendOutput(e.message.color(NamedTextColor.RED));
            }
        });

        return null;
    }
}
