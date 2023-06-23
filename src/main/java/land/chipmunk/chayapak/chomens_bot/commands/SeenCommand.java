package land.chipmunk.chayapak.chomens_bot.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.plugins.PlayersPlugin;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class SeenCommand implements Command {
    public String name() { return "seen"; }

    public String description() {
        return "Shows the last seen of a player";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<{player}>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("lastseen");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.PUBLIC;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final String player = String.join(" ", args);

        for (Bot eachBot : bot.bots()) {
            if (eachBot.players().getEntry(player) != null) return Component.empty()
                    .append(Component.text(player))
                    .append(Component.text(" is currently online on "))
                    .append(Component.text(eachBot.host() + ":" + eachBot.port()))
                    .color(NamedTextColor.RED);
        }

        final JsonElement playerElement = PlayersPlugin.playersObject().get(player);
        if (playerElement == null) return Component.translatable(
                "%s was never seen",
                Component.text(player)
        ).color(NamedTextColor.RED);

        final JsonObject lastSeen = playerElement.getAsJsonObject().get("lastSeen").getAsJsonObject();

        final DateTime dateTime = new DateTime(lastSeen.get("time").getAsLong(), DateTimeZone.UTC);

        final DateTimeFormatter formatter = DateTimeFormat.forPattern("EEEE, MMMM d, YYYY, hh:mm:ss a z");
        final String formattedTime = formatter.print(dateTime);

        final String server = lastSeen.get("server").getAsString();

        return Component.translatable(
                "%s was last seen at %s on %s",
                Component.text(player).color(ColorUtilities.getColorByString(bot.config().colorPalette().username())),
                Component.text(formattedTime).color(ColorUtilities.getColorByString(bot.config().colorPalette().string())),
                Component.text(server).color(ColorUtilities.getColorByString(bot.config().colorPalette().string()))
        ).color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor()));
    }
}
