package org.example.maniacrevolution.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Генератор лабиринта алгоритмом Recursive Backtracker (DFS).
 *
 * CELL_SIZE = 3:  проход = 2 блока шириной, стена = 1 блок
 * Вход/Выход:     2 блока шириной (= CELL_SIZE - 1)
 * Материалы:      тёмная тема — Blackstone, Crying Obsidian, Purpur
 * Крыша:          есть (Y = WALL_HEIGHT + 1 относительно пола)
 *
 * Размер в мире:  worldWidth() x (WALL_HEIGHT+2) x worldDepth()
 */
public class MazeGenerator {

    // ── Настройки ─────────────────────────────────────────────────────────────
    public static final int MAZE_W      = 9;  // клеток по X
    public static final int MAZE_H      = 9;  // клеток по Z
    public static final int CELL_SIZE   = 3;  // проход=2, стена=1
    public static final int WALL_HEIGHT = 5;  // высота (пол=Y0, крыша=Y6)

    /** Отступ безопасности вокруг лабиринта при проверке коллизий */
    public static final int COLLISION_PADDING = 4;

    // ── Направления: N=0  E=1  S=2  W=3 ──────────────────────────────────────
    private static final int[] DX       = { 0,  1,  0, -1 };
    private static final int[] DZ       = {-1,  0,  1,  0 };
    private static final int[] OPPOSITE = { 2,  3,  0,  1 };

    private final boolean[][][] walls;   // walls[cx][cz][dir]
    private final boolean[][]   visited;
    private final Random        rng;

    // ── Материалы ─────────────────────────────────────────────────────────────
    private static final BlockState WALL_MAT   = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState PILLAR_MAT = Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState FLOOR_MAT  = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState BORDER_MAT = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState ROOF_MAT   = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState ACCENT_MAT = Blocks.PURPUR_BLOCK.defaultBlockState();
    private static final BlockState AIR        = Blocks.AIR.defaultBlockState();

    // ── Конструктор ───────────────────────────────────────────────────────────
    public MazeGenerator(long seed) {
        this.rng     = new Random(seed);
        this.walls   = new boolean[MAZE_W][MAZE_H][4];
        this.visited = new boolean[MAZE_W][MAZE_H];

        for (boolean[][] row : walls)
            for (boolean[] cell : row)
                Arrays.fill(cell, true);

        generateDFS(0, 0);
    }

    private void generateDFS(int x, int z) {
        visited[x][z] = true;
        Integer[] dirs = {0, 1, 2, 3};
        List<Integer> shuffled = Arrays.asList(dirs);
        Collections.shuffle(shuffled, rng);
        for (int dir : shuffled) {
            int nx = x + DX[dir], nz = z + DZ[dir];
            if (inBounds(nx, nz) && !visited[nx][nz]) {
                walls[x][z][dir]             = false;
                walls[nx][nz][OPPOSITE[dir]] = false;
                generateDFS(nx, nz);
            }
        }
    }

    private boolean inBounds(int x, int z) {
        return x >= 0 && x < MAZE_W && z >= 0 && z < MAZE_H;
    }

    // ── Размеры ───────────────────────────────────────────────────────────────
    /** Ширина лабиринта по X (включая внешние стены) */
    public static int worldWidth() { return MAZE_W * CELL_SIZE + 1; }

    /** Глубина лабиринта по Z (включая внешние стены) */
    public static int worldDepth() { return MAZE_H * CELL_SIZE + 1; }

    // ── Строительство ─────────────────────────────────────────────────────────
    /**
     * Строит лабиринт в мире.
     * @param origin  нижний-левый угол, Y = уровень пола
     * @return список всех поставленных BlockPos (для сноса)
     */
    public List<BlockPos> buildInWorld(ServerLevel level, BlockPos origin) {
        List<BlockPos> placed = new ArrayList<>();

        int W     = worldWidth();
        int D     = worldDepth();
        int roofY = WALL_HEIGHT + 1;

        // ── ШАГ 1: Заполняем весь прямоугольник ──────────────────────────────
        for (int bx = 0; bx < W; bx++) {
            for (int bz = 0; bz < D; bz++) {
                boolean isBorder = (bx == 0 || bx == W - 1 || bz == 0 || bz == D - 1);
                boolean isCorner = (bx % CELL_SIZE == 0) && (bz % CELL_SIZE == 0);

                // Пол
                BlockState floorMat = isBorder ? BORDER_MAT : FLOOR_MAT;
                place(level, origin.offset(bx, 0, bz), floorMat, placed);

                // Стены Y=1..WALL_HEIGHT
                BlockState wallMat = isCorner ? PILLAR_MAT : WALL_MAT;
                for (int by = 1; by <= WALL_HEIGHT; by++)
                    place(level, origin.offset(bx, by, bz), wallMat, placed);

                // Крыша
                place(level, origin.offset(bx, roofY, bz), ROOF_MAT, placed);
            }
        }

        // ── ШАГ 2: Прорубаем проходы в клетках ───────────────────────────────
        for (int cx = 0; cx < MAZE_W; cx++) {
            for (int cz = 0; cz < MAZE_H; cz++) {
                int wx = cx * CELL_SIZE + 1; // X-начало прохода клетки
                int wz = cz * CELL_SIZE + 1; // Z-начало прохода клетки

                // Внутренность клетки 2×2
                for (int dx = 0; dx < CELL_SIZE - 1; dx++)
                    for (int dz = 0; dz < CELL_SIZE - 1; dz++)
                        clearPassage(level, origin, wx + dx, wz + dz, placed);

                // Восточный проход
                if (!walls[cx][cz][1] && cx + 1 < MAZE_W) {
                    int passX = cx * CELL_SIZE + CELL_SIZE;
                    for (int dz = 0; dz < CELL_SIZE - 1; dz++)
                        clearPassage(level, origin, passX, wz + dz, placed);
                }

                // Южный проход
                if (!walls[cx][cz][2] && cz + 1 < MAZE_H) {
                    int passZ = cz * CELL_SIZE + CELL_SIZE;
                    for (int dx = 0; dx < CELL_SIZE - 1; dx++)
                        clearPassage(level, origin, wx + dx, passZ, placed);
                }
            }
        }

        // ── ШАГ 3: Вход — 2 блока, северная стена (bz=0), X=1..2 ─────────────
        for (int dx = 0; dx < CELL_SIZE - 1; dx++) {
            clearPassage(level, origin, 1 + dx, 0, placed);
            // Purpur-акцент на крыше над входом
            level.setBlock(origin.offset(1 + dx, roofY, 0), ACCENT_MAT, 3);
        }

        // ── ШАГ 4: Выход — 2 блока, южная стена последней клетки ─────────────
        // Последняя клетка: cx = MAZE_W-1, cz = MAZE_H-1
        // Выход по Z: bz = MAZE_H*CELL_SIZE (южная внешняя стена)
        // Выход по X: начинается с (MAZE_W-1)*CELL_SIZE + 1
        int exitX = (MAZE_W - 1) * CELL_SIZE + 1;
        int exitZ = MAZE_H * CELL_SIZE;
        for (int dx = 0; dx < CELL_SIZE - 1; dx++) {
            clearPassage(level, origin, exitX + dx, exitZ, placed);
            // Purpur-акцент на крыше над выходом
            level.setBlock(origin.offset(exitX + dx, roofY, exitZ), ACCENT_MAT, 3);
        }

        return placed;
    }

    /**
     * Убирает воздух в столбце (Y=1..WALL_HEIGHT) по относительным coords bx, bz.
     * Пол (Y=0) и крыша (Y=roofY) остаются нетронутыми.
     */
    private void clearPassage(ServerLevel level, BlockPos origin, int bx, int bz,
                              List<BlockPos> placed) {
        for (int by = 1; by <= WALL_HEIGHT; by++) {
            BlockPos p = origin.offset(bx, by, bz);
            level.setBlock(p, AIR, 3);
            // placed здесь не добавляем: MazeManager.demolish() удаляет
            // весь прямоугольник через bounding box, а не по списку проходов
        }
    }

    private void place(ServerLevel level, BlockPos pos, BlockState state, List<BlockPos> placed) {
        level.setBlock(pos, state, 3);
        placed.add(pos.immutable());
    }

    // ── Геттеры ───────────────────────────────────────────────────────────────
    /** Позиция телепорта игрока (перед входом) */
    public BlockPos getEntryPoint(BlockPos origin) {
        return origin.offset(1, 1, -1);
    }

    /** Позиция сразу за выходом */
    public BlockPos getExitPoint(BlockPos origin) {
        int exitX = (MAZE_W - 1) * CELL_SIZE + 1;
        int exitZ = MAZE_H * CELL_SIZE + 1;
        return origin.offset(exitX, 1, exitZ);
    }
}