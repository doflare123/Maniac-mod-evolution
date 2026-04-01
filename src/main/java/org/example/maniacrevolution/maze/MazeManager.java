package org.example.maniacrevolution.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/**
 * Менеджер активных лабиринтов.
 *
 * Ключевая фича: перед размещением нового лабиринта проверяем,
 * не пересекается ли он с уже существующими. Если да — ищем
 * свободное место, сдвигаясь по X с шагом (worldWidth + PADDING).
 *
 * Снос: MazeManager хранит bounding box каждого лабиринта и при
 * demolish() заполняет весь прямоугольник воздухом — быстро и надёжно.
 */
public class MazeManager {

    // ── Синглтон ──────────────────────────────────────────────────────────────
    private static final MazeManager INSTANCE = new MazeManager();
    public static MazeManager getInstance() { return INSTANCE; }
    private MazeManager() {}

    // ── 60 секунд = 1200 тиков ────────────────────────────────────────────────
    private static final long LIFETIME_TICKS = 60 * 20L;

    // ── Структура активного лабиринта ─────────────────────────────────────────
    private static class ActiveMaze {
        final ServerLevel level;
        final BlockPos    origin;      // нижний-левый угол (Y = пол)
        final int         width;       // по X в блоках
        final int         depth;       // по Z в блоках
        final int         totalHeight; // WALL_HEIGHT + 2 (пол + крыша)
        long              destroyAtTick;

        ActiveMaze(ServerLevel level, BlockPos origin, int width, int depth,
                   int totalHeight, long destroyAtTick) {
            this.level         = level;
            this.origin        = origin;
            this.width         = width;
            this.depth         = depth;
            this.totalHeight   = totalHeight;
            this.destroyAtTick = destroyAtTick;
        }

        /**
         * Проверяет, пересекается ли этот лабиринт с кандидатом.
         * Учитывает COLLISION_PADDING с каждой стороны.
         */
        boolean overlaps(BlockPos candOrigin, int candW, int candD) {
            int pad = MazeGenerator.COLLISION_PADDING;

            int aMinX = origin.getX() - pad,     aMaxX = origin.getX() + width + pad;
            int aMinZ = origin.getZ() - pad,     aMaxZ = origin.getZ() + depth + pad;

            int bMinX = candOrigin.getX(),        bMaxX = candOrigin.getX() + candW;
            int bMinZ = candOrigin.getZ(),        bMaxZ = candOrigin.getZ() + candD;

            // AABB overlap по X и Z
            return aMinX < bMaxX && aMaxX > bMinX
                    && aMinZ < bMaxZ && aMaxZ > bMinZ;
        }
    }

    private final Map<UUID, ActiveMaze> activeMazes = new LinkedHashMap<>();

    // ── Публичный API ─────────────────────────────────────────────────────────

    /**
     * Находит свободное место рядом с preferredOrigin и строит лабиринт.
     *
     * Алгоритм поиска свободного места:
     *   1. Пробуем preferredOrigin
     *   2. Если занято — сдвигаемся по X на (worldWidth + PADDING)
     *   3. До MAX_ATTEMPTS попыток
     *
     * @param preferredOrigin желаемая позиция (нижний-левый угол)
     * @param seed            seed генерации
     * @return UUID нового лабиринта, или null если не нашли места
     */
    public UUID spawnMaze(ServerLevel level, BlockPos preferredOrigin, long seed) {
        int W     = MazeGenerator.worldWidth();
        int D     = MazeGenerator.worldDepth();
        int H     = MazeGenerator.WALL_HEIGHT + 2;
        int step  = W + MazeGenerator.COLLISION_PADDING;

        BlockPos chosenOrigin = null;
        int MAX_ATTEMPTS = 20;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            // Чередуем: attempt 0 → +0, attempt 1 → +step, attempt 2 → -step,
            //           attempt 3 → +2*step, attempt 4 → -2*step, ...
            int offset = (attempt % 2 == 0) ? (attempt / 2) * step : -((attempt + 1) / 2) * step;
            BlockPos candidate = preferredOrigin.offset(offset, 0, 0);

            if (!collidesWithAny(candidate, W, D)) {
                chosenOrigin = candidate;
                break;
            }
        }

        if (chosenOrigin == null) {
            // Аварийный вариант: уходим далеко по Z
            chosenOrigin = preferredOrigin.offset(0, 0, (activeMazes.size() + 1) * (D + 10));
        }

        // Строим
        MazeGenerator gen = new MazeGenerator(seed);
        gen.buildInWorld(level, chosenOrigin);

        long currentTick = level.getGameTime();
        UUID id = UUID.randomUUID();
        activeMazes.put(id, new ActiveMaze(level, chosenOrigin, W, D, H,
                currentTick + LIFETIME_TICKS));
        return id;
    }

    /** Принудительный снос */
    public void destroyMaze(UUID id) {
        ActiveMaze maze = activeMazes.remove(id);
        if (maze != null) demolish(maze);
    }

    /** Вызывается каждый серверный тик из MazeTickHandler */
    public void tick(long currentTick) {
        Iterator<Map.Entry<UUID, ActiveMaze>> it = activeMazes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ActiveMaze> entry = it.next();
            if (currentTick >= entry.getValue().destroyAtTick) {
                demolish(entry.getValue());
                it.remove();
            }
        }
    }

    /** Секунд до сноса лабиринта (−1 если не найден) */
    public long getSecondsLeft(UUID id, long currentTick) {
        ActiveMaze m = activeMazes.get(id);
        if (m == null) return -1;
        return Math.max(0, (m.destroyAtTick - currentTick) / 20);
    }

    public int activeCount() { return activeMazes.size(); }

    /**
     * Возвращает реально использованный origin для последнего spawn.
     * Нужен MazeSpawnerItem чтобы сообщить точные координаты игроку.
     */
    public BlockPos getOrigin(UUID id) {
        ActiveMaze m = activeMazes.get(id);
        return m != null ? m.origin : null;
    }

    // ── Внутренние методы ─────────────────────────────────────────────────────

    private boolean collidesWithAny(BlockPos candidate, int W, int D) {
        for (ActiveMaze m : activeMazes.values()) {
            if (m.overlaps(candidate, W, D)) return true;
        }
        return false;
    }

    /**
     * Сносит лабиринт: заполняет весь bounding box воздухом.
     * Проходим сверху вниз, чтобы гравитационные блоки (Sand и т.п.)
     * не падали на ещё не убранные.
     */
    private void demolish(ActiveMaze maze) {
        int W = maze.width;
        int D = maze.depth;
        int H = maze.totalHeight;

        for (int by = H; by >= 0; by--) {
            for (int bx = 0; bx < W; bx++) {
                for (int bz = 0; bz < D; bz++) {
                    maze.level.setBlock(
                            maze.origin.offset(bx, by, bz),
                            Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}