package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.util.MazeGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class GenerateMazeCommand extends Command {
    public String name = "generatemaze";

    public String description = "Generates a maze";

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

    public int trustLevel = 0;

    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot();

        try {
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);

            final int width = Integer.parseInt(args[3]);
            final int height = Integer.parseInt(args[4]);

            final MazeGenerator generator = new MazeGenerator(width, height);

            generator.generateMaze();

            bot.maze().generate(generator, x, y, z);
        } catch (NumberFormatException e) {
            return Component.text("Invalid position/size").color(NamedTextColor.RED);
        }

        return null;
    }
}
