package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ComponentUtilities;
import land.chipmunk.chayapak.chomens_bot.util.NumberUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class RandomTeleportCommand implements Command {
    public String name() { return "rtp"; }

    public String description() {
        return "Randomly teleports you";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("randomteleport");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        final String stringDisplayName = ComponentUtilities.stringify(context.displayName());

        final int positionX = NumberUtilities.between(1_000, 10_000);
        final int positionZ = NumberUtilities.between(1_000, 10_000);
        final String stringPosition = positionX + " 100 " + positionZ; // very 100 y

        context.sendOutput(
                Component.empty()
                        .append(Component.text("Teleporting "))
                        .append(context.displayName().color(NamedTextColor.AQUA))
                        .append(Component.text(" to ").color(NamedTextColor.WHITE))
                        .append(Component.text(stringPosition).color(NamedTextColor.GREEN))
                        .append(Component.text("...").color(NamedTextColor.WHITE))
        );

        // TODO: Use UUID instead of display name because it is a bad idea!!11
        bot.core().run("essentials:teleport " + stringDisplayName + " " + stringPosition);

        return Component.text("success");
    }
}
