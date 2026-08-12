package org.example.maniacrevolution.maze;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MazeManager {
    private static final MazeManager INSTANCE = new MazeManager();
    private static final long LIFETIME_TICKS = 60 * 20L;
    private static final int BULK_UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    private final Map<UUID, ActiveMaze> activeMazes = new LinkedHashMap<>();

    public static MazeManager getInstance() {
        return INSTANCE;
    }

    private MazeManager() {
    }

    public UUID spawnMaze(ServerLevel level, BlockPos preferredOrigin, long seed) {
        int width = MazeGenerator.worldWidth();
        int depth = MazeGenerator.worldDepth();
        int step = width + MazeGenerator.COLLISION_PADDING;
        BlockPos chosenOrigin = null;

        for (int attempt = 0; attempt < 20; attempt++) {
            int offset = attempt % 2 == 0
                    ? attempt / 2 * step
                    : -((attempt + 1) / 2) * step;
            BlockPos candidate = preferredOrigin.offset(offset, 0, 0);
            if (!collidesWithAny(level, candidate, width, depth)) {
                chosenOrigin = candidate;
                break;
            }
        }

        if (chosenOrigin == null) {
            chosenOrigin = preferredOrigin.offset(0, 0, (activeMazes.size() + 1) * (depth + 10));
        }

        MazeGenerator generator = new MazeGenerator(seed);
        Map<BlockPos, BlockState> originalBlocks = generator.buildInWorld(level, chosenOrigin);
        UUID id = UUID.randomUUID();
        activeMazes.put(id, new ActiveMaze(
                level, chosenOrigin, width, depth, originalBlocks, level.getGameTime() + LIFETIME_TICKS));
        return id;
    }

    public void destroyMaze(UUID id) {
        ActiveMaze maze = activeMazes.remove(id);
        if (maze != null) {
            demolish(maze);
        }
    }

    public void tick(long currentTick) {
        Iterator<Map.Entry<UUID, ActiveMaze>> iterator = activeMazes.entrySet().iterator();
        while (iterator.hasNext()) {
            ActiveMaze maze = iterator.next().getValue();
            if (currentTick >= maze.destroyAtTick) {
                demolish(maze);
                iterator.remove();
            }
        }
    }

    public long getSecondsLeft(UUID id, long currentTick) {
        ActiveMaze maze = activeMazes.get(id);
        return maze == null ? -1 : Math.max(0, (maze.destroyAtTick - currentTick) / 20);
    }

    public int activeCount() {
        return activeMazes.size();
    }

    public BlockPos getOrigin(UUID id) {
        ActiveMaze maze = activeMazes.get(id);
        return maze == null ? null : maze.origin;
    }

    private boolean collidesWithAny(ServerLevel level, BlockPos candidate, int width, int depth) {
        for (ActiveMaze maze : activeMazes.values()) {
            if (maze.level == level && maze.overlaps(candidate, width, depth)) {
                return true;
            }
        }
        return false;
    }

    private void demolish(ActiveMaze maze) {
        List<Map.Entry<BlockPos, BlockState>> changed = new ArrayList<>(maze.originalBlocks.entrySet());
        for (int i = changed.size() - 1; i >= 0; i--) {
            Map.Entry<BlockPos, BlockState> entry = changed.get(i);
            maze.level.setBlock(entry.getKey(), entry.getValue(), BULK_UPDATE_FLAGS);
        }
        maze.originalBlocks.clear();
    }

    private static final class ActiveMaze {
        private final ServerLevel level;
        private final BlockPos origin;
        private final int width;
        private final int depth;
        private final Map<BlockPos, BlockState> originalBlocks;
        private final long destroyAtTick;

        private ActiveMaze(ServerLevel level, BlockPos origin, int width, int depth,
                           Map<BlockPos, BlockState> originalBlocks, long destroyAtTick) {
            this.level = level;
            this.origin = origin;
            this.width = width;
            this.depth = depth;
            this.originalBlocks = originalBlocks;
            this.destroyAtTick = destroyAtTick;
        }

        private boolean overlaps(BlockPos candidate, int candidateWidth, int candidateDepth) {
            int padding = MazeGenerator.COLLISION_PADDING;
            int minX = origin.getX() - padding;
            int maxX = origin.getX() + width + padding;
            int minZ = origin.getZ() - padding;
            int maxZ = origin.getZ() + depth + padding;
            int candidateMaxX = candidate.getX() + candidateWidth;
            int candidateMaxZ = candidate.getZ() + candidateDepth;
            return minX < candidateMaxX && maxX > candidate.getX()
                    && minZ < candidateMaxZ && maxZ > candidate.getZ();
        }
    }
}
