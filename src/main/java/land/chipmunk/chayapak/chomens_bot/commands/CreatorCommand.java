package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.ColorUtilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class CreatorCommand implements Command {
    public String name() { return "creator"; }

    public String description() {
        return "Shows the bot's creator";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("");

        return aliases;
    }

    public int trustLevel() {
        return 0;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        return Component.empty()
                .append(Component.text("ChomeNS Bot ").color(ColorUtilities.getColorByString(bot.config().colorPalette().primary())))
                .append(Component.text("was created by ").color(ColorUtilities.getColorByString(bot.config().colorPalette().defaultColor())))
                .append(Component.text("chayapak").color(ColorUtilities.getColorByString(bot.config().colorPalette().ownerName())));
    }
}
