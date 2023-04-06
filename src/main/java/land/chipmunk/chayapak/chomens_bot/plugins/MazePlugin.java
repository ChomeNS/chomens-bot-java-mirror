package land.chipmunk.chayapak.chomens_bot.plugins;

import land.chipmunk.chayapak.chomens_bot.Bot;
import land.chipmunk.chayapak.chomens_bot.util.MazeGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class MazePlugin {
    private final Bot bot;

    public MazePlugin (Bot bot) {
        this.bot = bot;
    }

    // also totally didn't ask chatgpt for this too (but modified a bit)
    public void generate (MazeGenerator generator, int startX, int startY, int startZ) {
        bot.chat().tellraw(
                Component.translatable(
                        "Generating maze at %s %s %s...",
                        Component.text(startX).color(NamedTextColor.AQUA),
                        Component.text(startY).color(NamedTextColor.AQUA),
                        Component.text(startZ).color(NamedTextColor.AQUA)
                )
        );

        final int[][] maze = generator.maze();

        int x = startX;
        int z = startZ;

        // Find the starting and ending positions of the maze
        int startRow = 0;
        int startCol = 0;
        int endRow = maze.length - 1;
        int endCol = maze[0].length - 1;
        while (maze[startRow][startCol] != 0) {
            startCol++;
            if (startCol == maze[0].length) {
                startCol = 0;
                startRow++;
            }
        }
        while (maze[endRow][endCol] != 0) {
            endCol--;
            if (endCol < 0) {
                endCol = maze[0].length - 1;
                endRow--;
            }
        }

        final String command = "minecraft:fill %s %s %s %s %s %s %s";

        // fill the floor
        bot.core().run(
                String.format(
                        command,
                        x,
                        startY - 1,
                        z,
                        x + generator.width(),
                        startY - 1,
                        z + generator.height(),
                        "minecraft:stone_bricks replace minecraft:air"
                )
        );

        // actually build the maze
        for (int row = 0; row < generator.height(); row++) {
            for (int col = 0; col < generator.width(); col++) {
                if (maze[row][col] == 1) {
                    // makes the wall
                    bot.core().run(
                            String.format(
                                    command,
                                    x,
                                    startY,
                                    z,
                                    x,
                                    startY + 3,
                                    z,
                                    "minecraft:stone"
                            )
                    );
                } else if ((row == startRow && col == startCol)) {
                    // Set a marker block for the start position
                    bot.core().run(
                            String.format(
                                    command,
                                    x,
                                    startY - 1,
                                    z,
                                    x,
                                    startY - 1,
                                    z,
                                    "minecraft:glowstone"
                            )
                    );
                } else if ((row == endRow && col == endCol)) {
                    // Set a marker block for the end position
                    bot.core().run(
                            String.format(
                                    command,
                                    x,
                                    startY - 1,
                                    z,
                                    x,
                                    startY - 1,
                                    z,
                                    "minecraft:lime_concrete"
                            )
                    );
                }

                // Increment the x-coordinate
                x++;

                // If we've reached the end of the x-axis, reset x and increment z
                if (x == startX + maze[0].length) {
                    x = startX;
                    z++;
                }
            }
        }

        // lazy fix for the sus border issue
        bot.core().run(
                String.format(
                        command,
                        x + generator.width(),
                        startY,
                        z,
                        x + generator.width(),
                        startY + 3,
                        z + generator.height(),
                        "minecraft:stone"
                )
        );

        bot.core().run(
                String.format(
                        command,
                        x,
                        startY,
                        z + generator.height(),
                        x + generator.width(),
                        startY + 3,
                        z + generator.height(),
                        "minecraft:stone"
                )
        );

        bot.chat().tellraw(
                Component.empty()
                        .append(Component.text("Done generating maze. "))
                        .append(
                                Component
                                        .text("Click here to teleport")
                                        .color(NamedTextColor.GREEN)
                                        .clickEvent(
                                                ClickEvent.runCommand(
                                                        String.format(
                                                                "/tp %s %s %s",
                                                                startX,
                                                                startY + 4,
                                                                startZ
                                                        )
                                                )
                                        )
                        )
        );
    }
}
