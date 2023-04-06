package land.chipmunk.chayapak.chomens_bot.util;

import lombok.Getter;

import java.util.*;

// totally didn't ask chatgpt for this lmao
public class MazeGenerator {
    @Getter private final int width;
    @Getter private final int height;
    @Getter private final int[][] maze;
    private final Random rand;

    public MazeGenerator(int width, int height) {
        this.width = width;
        this.height = height;
        this.maze = new int[height][width];
        this.rand = new Random();
    }

    public void generateMaze() {
        // Set all cells to walls
        for (int row = 0; row < height; row++) {
            Arrays.fill(maze[row], 1);
        }

        // Create a starting point
        int startX = rand.nextInt(width);
        int startY = rand.nextInt(height);
        maze[startY][startX] = 0;

        // Recursive backtracking algorithm
        backtrack(startX, startY);
    }

    private void backtrack(int x, int y) {
        // Get a list of neighboring cells
        List<int[]> neighbors = getNeighbors(x, y);

        // Shuffle the list of neighbors
        Collections.shuffle(neighbors, rand);

        for (int[] neighbor : neighbors) {
            int nx = neighbor[0];
            int ny = neighbor[1];

            // Check if the neighboring cell is a wall
            if (maze[ny][nx] == 1) {
                // Remove the wall between the current cell and the neighboring cell
                maze[(y + ny) / 2][(x + nx) / 2] = 0;
                maze[ny][nx] = 0;

                // Recursively backtrack from the neighboring cell
                backtrack(nx, ny);
            }
        }
    }

    private List<int[]> getNeighbors(int x, int y) {
        List<int[]> neighbors = new ArrayList<>();

        if (x > 1) {
            neighbors.add(new int[]{x - 2, y});
        }
        if (y > 1) {
            neighbors.add(new int[]{x, y - 2});
        }
        if (x < width - 2) {
            neighbors.add(new int[]{x + 2, y});
        }
        if (y < height - 2) {
            neighbors.add(new int[]{x, y + 2});
        }

        return neighbors;
    }
}

