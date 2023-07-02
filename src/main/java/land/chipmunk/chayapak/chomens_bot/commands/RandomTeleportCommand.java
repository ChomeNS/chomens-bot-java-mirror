package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.data.chat.MutablePlayerListEntry;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import land.chipmunk.chayapak.chomens_bot.util.MathUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RandomTeleportCommand extends Command {
    public RandomTeleportCommand () {
        super(
                "rtp",
                "Randomly teleports you",
                new String[] {},
                new String[] { "randomteleport" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        final MutablePlayerListEntry sender = context.sender;

        final int positionX = MathUtilities.between(1_000, 1_000_000);
        final int positionZ = MathUtilities.between(1_000, 1_000_000);
        final String stringPosition = positionX + " 100 " + positionZ; // very 100 y

        bot.core.run("essentials:teleport " + sender.profile.getIdAsString() + " " + stringPosition);

        return Component.empty()
                .append(Component.text("Teleporting "))
                .append(Component.text(sender.profile.getName()).color(ColorUtilities.getColorByString(bot.config.colorPalette.username)))
                .append(Component.text(" to ").color(NamedTextColor.WHITE))
                .append(Component.text(stringPosition).color(NamedTextColor.GREEN))
                .append(Component.text("...").color(NamedTextColor.WHITE))
                .color(ColorUtilities.getColorByString(bot.config.colorPalette.defaultColor));
    }
}
