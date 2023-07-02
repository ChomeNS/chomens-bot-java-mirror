package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

public class CreatorCommand extends Command {
    public CreatorCommand () {
        super(
                "creator",
                "Shows the bots creator",
                new String[] {},
                new String[] {},
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        return Component.empty()
                .append(Component.text("ChomeNS Bot ").color(ColorUtilities.getColorByString(bot.config.colorPalette.primary)))
                .append(Component.text("is created by ").color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor)))
                .append(Component.text("chayapak").color(ColorUtilities.getColorByString(bot.config.colorPalette.ownerName)));
    }
}
