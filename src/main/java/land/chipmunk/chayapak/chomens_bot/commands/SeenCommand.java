package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.plugins.PlayersPersistentDataPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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
    public Component execute(CommandContext context) throws Exception {
        final Bot bot = context.bot;

        final String player = context.getString(true, true);

        for (Bot eachBot : bot.bots) {
            if (eachBot.players.getEntry(player) != null) return Component.empty()
                    .append(Component.text(player))
                    .append(Component.text(" is currently online on "))
                    .append(Component.text(eachBot.host + ":" + eachBot.port))
                    .color(NamedTextColor.RED);
        }

        final JsonElement playerElement = PlayersPersistentDataPlugin.playersObject.get(player);
        if (playerElement == null) return Component.translatable(
                "%s was never seen",
                Component.text(player)
        ).color(NamedTextColor.RED);

        final JsonObject lastSeen = playerElement.getAsJsonObject().get("lastSeen").getAsJsonObject();

        final JsonElement time = lastSeen.get("time");

        if (time == null) throw new CommandException(Component.text("time is null.. (possible invalid entry)"));

        final DateTime dateTime = new DateTime(time.getAsLong(), DateTimeZone.UTC);

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE, MMMM d, YYYY, hh:mm:ss a z");
        final String formattedTime = formatter.print(dateTime);

        final String server = lastSeen.get("server").getAsString();

        return Component.translatable(
                "%s was last seen at %s on %s",
                Component.text(player).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)),
                Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config.colorPalette.string)),
                Component.text(server).color(ColorUtilities.getColorByString(bot.config.colorPalette.string))
        ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
