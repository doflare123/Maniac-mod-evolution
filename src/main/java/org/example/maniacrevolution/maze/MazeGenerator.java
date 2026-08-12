package org.example.maniacrevolution.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MazeGenerator {
    public static final int MAZE_W = 9;
    public static final int MAZE_H = 9;
    public static final int CELL_SIZE = 3;
    public static final int WALL_HEIGHT = 5;
    public static final int COLLISION_PADDING = 4;

    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DZ = {-1, 0, 1, 0};
    private static final int[] OPPOSITE = {2, 3, 0, 1};
    private static final int BULK_UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    private static final BlockState WALL_MAT = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState PILLAR_MAT = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState FLOOR_MAT = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState BORDER_MAT = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState ROOF_MAT = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState ACCENT_MAT = Blocks.PURPUR_BLOCK.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private final boolean[][][] walls;
    private final boolean[][] visited;
    private final Random random;

    public MazeGenerator(long seed) {
        random = new Random(seed);
        walls = new boolean[MAZE_W][MAZE_H][4];
        visited = new boolean[MAZE_W][MAZE_H];
        for (boolean[][] row : walls) {
            for (boolean[] cell : row) {
                Arrays.fill(cell, true);
            }
        }
        generateDepthFirst(0, 0);
    }

    private void generateDepthFirst(int x, int z) {
        visited[x][z] = true;
        List<Integer> directions = Arrays.asList(0, 1, 2, 3);
        Collections.shuffle(directions, random);
        for (int direction : directions) {
            int nextX = x + DX[direction];
            int nextZ = z + DZ[direction];
            if (inBounds(nextX, nextZ) && !visited[nextX][nextZ]) {
                walls[x][z][direction] = false;
                walls[nextX][nextZ][OPPOSITE[direction]] = false;
                generateDepthFirst(nextX, nextZ);
            }
        }
    }

    private boolean inBounds(int x, int z) {
        return x >= 0 && x < MAZE_W && z >= 0 && z < MAZE_H;
    }

    public static int worldWidth() {
        return MAZE_W * CELL_SIZE + 1;
    }

    public static int worldDepth() {
        return MAZE_H * CELL_SIZE + 1;
    }

    /**
     * Writes every maze coordinate at most once in its final state. The returned
     * snapshot contains only blocks that actually changed and is used for cleanup.
     */
    public Map<BlockPos, BlockState> buildInWorld(ServerLevel level, BlockPos origin) {
        Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();
        int width = worldWidth();
        int depth = worldDepth();
        int roofY = WALL_HEIGHT + 1;
        int exitX = (MAZE_W - 1) * CELL_SIZE + 1;
        int exitZ = MAZE_H * CELL_SIZE;

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                boolean border = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                place(level, origin.offset(x, 0, z), border ? BORDER_MAT : FLOOR_MAT, originalBlocks);

                BlockState roof = isEntranceOrExitColumn(x, z, exitX, exitZ) ? ACCENT_MAT : ROOF_MAT;
                place(level, origin.offset(x, roofY, z), roof, originalBlocks);

                BlockState column = passageAt(x, z) ? AIR : wallMaterial(x, z, exitX, exitZ);
                for (int y = 1; y <= WALL_HEIGHT; y++) {
                    place(level, origin.offset(x, y, z), column, originalBlocks);
                }
            }
        }
        return originalBlocks;
    }

    private boolean passageAt(int x, int z) {
        if (x <= 0 || x >= worldWidth() - 1 || z <= 0 || z >= worldDepth() - 1) {
            return false;
        }

        int xMod = x % CELL_SIZE;
        int zMod = z % CELL_SIZE;
        if (xMod != 0 && zMod != 0) {
            return true;
        }
        if (xMod == 0 && zMod == 0) {
            return false;
        }

        if (xMod == 0) {
            int westCell = x / CELL_SIZE - 1;
            int cellZ = (z - 1) / CELL_SIZE;
            return westCell >= 0 && westCell + 1 < MAZE_W && !walls[westCell][cellZ][1];
        }

        int northCell = z / CELL_SIZE - 1;
        int cellX = (x - 1) / CELL_SIZE;
        return northCell >= 0 && northCell + 1 < MAZE_H && !walls[cellX][northCell][2];
    }

    private static BlockState wallMaterial(int x, int z, int exitX, int exitZ) {
        if (isEntranceOrExitColumn(x, z, exitX, exitZ)) {
            return BORDER_MAT;
        }
        return x % CELL_SIZE == 0 && z % CELL_SIZE == 0 ? PILLAR_MAT : WALL_MAT;
    }

    private static boolean isEntranceOrExitColumn(int x, int z, int exitX, int exitZ) {
        boolean entrance = z == 0 && x >= 1 && x < CELL_SIZE;
        boolean exit = z == exitZ && x >= exitX && x < exitX + CELL_SIZE - 1;
        return entrance || exit;
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state,
                              Map<BlockPos, BlockState> originalBlocks) {
        BlockState current = level.getBlockState(pos);
        if (current == state || current.equals(state)) {
            return;
        }
        originalBlocks.putIfAbsent(pos.immutable(), current);
        level.setBlock(pos, state, BULK_UPDATE_FLAGS);
    }

    public BlockPos getEntryPoint(BlockPos origin) {
        return origin.offset(1, 1, 1);
    }

    public BlockPos getExitPoint(BlockPos origin) {
        int exitX = (MAZE_W - 1) * CELL_SIZE + 1;
        int exitZ = (MAZE_H - 1) * CELL_SIZE + 1;
        return origin.offset(exitX, 1, exitZ);
    }
}
