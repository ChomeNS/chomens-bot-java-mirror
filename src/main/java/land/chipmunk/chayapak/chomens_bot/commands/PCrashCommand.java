package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.PlayerEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class PCrashCommand extends Command {
    public PCrashCommand () {
        super(
                "pcrash",
                "Crashes a player using particle",
                new String[] { "<player>" },
                new String[] { "particlecrash" },
                TrustLevel.TRUSTED,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        final Bot bot = context.bot;

        final PlayerEntry player = bot.players.getEntry(context.getString(true, true));

        if (player == null) throw new CommandException(Component.text("Invalid player name"));

        bot.exploits.pcrash(player.profile.getId());

        return Component.translatable(
                "Attempting to crash %s",
                Component.text(player.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.secondary))
        ).color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
