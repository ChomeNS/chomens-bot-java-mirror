package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.CommandException;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

public class StopCommand extends Command {
    public StopCommand () {
        super(
                "stop",
                "Gracefully stops the bot",
                new String[] { "<ownerHash>" },
                new String[] {},
                TrustLevel.OWNER,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        System.exit(0);

        return Component.text("Stopping").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
