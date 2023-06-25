package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.MazeGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class GenerateMazeCommand implements Command {
    public String name() { return "generatemaze"; }

    public String description() {
        return "Generates a maze";
    }

    public List<String> usage() {
        final List<String> usages = new ArrayList<>();
        usages.add("<x> <y> <z> <width> <long>");

        return usages;
    }

    public List<String> alias() {
        final List<String> aliases = new ArrayList<>();
        aliases.add("genmaze");
        aliases.add("mazegen");

        return aliases;
    }

    public TrustLevel trustLevel() {
        return TrustLevel.PUBLIC;
    }

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        try {
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);

            final int width = Integer.parseInt(args[3]);
            final int height = Integer.parseInt(args[4]);

            if (width > 100 || height > 100) return Component.text("Size is too big").color(NamedTextColor.RED);

            final MazeGenerator generator = new MazeGenerator(width, height);

            generator.generateMaze();

            bot.maze().generate(generator, x, y, z);
        } catch (NumberFormatException e) {
            return Component.text("Invalid position/size").color(NamedTextColor.RED);
        }

        return null;
    }
}
