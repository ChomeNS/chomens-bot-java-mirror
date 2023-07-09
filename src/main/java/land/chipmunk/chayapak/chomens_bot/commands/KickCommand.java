package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

public class KickCommand extends Command {
    public KickCommand () {
        super(
                "kick",
                "Kicks a player",
                new String[] { "<hash> <{player}>" },
                new String[] {},
                TrustLevel.TRUSTED
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        final PlayerEntry entry = bot.players.getEntry(String.join(" ", args));

        if (entry == null) return Component.text("Invalid player name").color(NamedTextColor.RED);

        final String name = entry.profile.getName();
        final UUID uuid = entry.profile.getId();

        bot.exploits.kick(uuid);

        return Component.empty()
                .append(Component.text("Kicking player "))
                .append(Component.text(name).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
