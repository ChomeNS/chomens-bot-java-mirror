package me.chayapak1.chomens_bot.commands;

import me.chayapak1.chomens_bot.Bot;
import me.chayapak1.chomens_bot.command.Command;
import me.chayapak1.chomens_bot.command.CommandContext;
import me.chayapak1.chomens_bot.command.CommandException;
import me.chayapak1.chomens_bot.command.TrustLevel;
import me.chayapak1.chomens_bot.data.PlayerEntry;
import me.chayapak1.chomens_bot.util.ColorUtilities;
import me.chayapak1.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RandomTeleportCommand extends Command {
    public RandomTeleportCommand () {
        super(
                "rtp",
                "Randomly teleports you",
                new String[] {},
                new String[] { "randomteleport" },
                TrustLevel.PUBLIC,
                false
        );
    }

    @Override
    public Component execute(CommandContext context) throws CommandException {
        context.checkOverloadArgs(0);

        final Bot bot = context.bot;

        final PlayerEntry sender = context.sender;

        final int positionX = MathUtilities.between(-1_000_000, 1_000_000);
        final int positionZ = MathUtilities.between(-1_000_000, 1_000_000);
        final String stringPosition = positionX + " 100 " + positionZ; // is hardcoding the y to 100 a great idea?

        bot.core.run("essentials:teleport " + sender.profile.getIdAsString() + " " + stringPosition);

        return Component.empty()
                .append(Component.text("Teleporting "))
                .append(Component.text(sender.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)))
                .append(Component.text(" to "))
                .append(Component.text(stringPosition).color(NamedTextColor.GREEN))
                .append(Component.text("..."))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
