package land.chipmunk.chayapak.chomens_bot.commands;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.command.Command;
import land.chipmunk.chayapak.chomens_bot.command.CommandContext;
import land.chipmunk.chayapak.chomens_bot.command.TrustLevel;
import land.chipmunk.chayapak.chomens_bot.util.MazeGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GenerateMazeCommand extends Command {
    public GenerateMazeCommand () {
        super(
                "generatemaze",
                "Generates a maze",
                new String[] { "<x> <y> <z> <width> <long>" },
                new String[] { "genmaze", "mazegen" },
                TrustLevel.PUBLIC
        );
    }

    @Override
    public Component execute(CommandContext context, String[] args, String[] fullArgs) {
        final Bot bot = context.bot;

        try {
            final int x = Integer.parseInt(args[0]);
            final int y = Integer.parseInt(args[1]);
            final int z = Integer.parseInt(args[2]);

            final int width = Integer.parseInt(args[3]);
            final int height = Integer.parseInt(args[4]);

            if (width > 100 || height > 100) return Component.text("Size is too big").color(NamedTextColor.RED);

            final MazeGenerator generator = new MazeGenerator(width, height);

            generator.generateMaze();

            bot.maze.generate(generator, x, y, z);
        } catch (NumberFormatException e) {
            return Component.text("Invalid position/size").color(NamedTextColor.RED);
        }

        return null;
    }
}
