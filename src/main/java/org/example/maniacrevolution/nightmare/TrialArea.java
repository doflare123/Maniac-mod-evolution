package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TrialArea {
    private static final int BULK_UPDATE_FLAGS = Block.UPDATE_CLIENTS;

    final ServerLevel level;
    final BlockPos origin;
    final int width;
    final int height;
    final int depth;
    final List<Entity> entities = new ArrayList<>();
    private final Map<BlockPos, BlockState> originalBlocks = new LinkedHashMap<>();

    TrialArea(ServerLevel level, BlockPos origin, int width, int height, int depth) {
        this.level = level;
        this.origin = origin;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    void setBlock(BlockPos relativePos, BlockState state) {
        setWorldBlock(origin.offset(relativePos), state);
    }

    void setBlock(int x, int y, int z, BlockState state) {
        setWorldBlock(origin.offset(x, y, z), state);
    }

    private void setWorldBlock(BlockPos worldPos, BlockState state) {
        BlockState current = level.getBlockState(worldPos);
        if (current == state || current.equals(state)) {
            return;
        }

        originalBlocks.putIfAbsent(worldPos.immutable(), current);
        level.setBlock(worldPos, state, BULK_UPDATE_FLAGS);
    }

    void destroy() {
        for (Entity entity : entities) {
            if (entity.isAlive()) {
                entity.discard();
            }
        }

        List<Map.Entry<BlockPos, BlockState>> changed = new ArrayList<>(originalBlocks.entrySet());
        for (int i = changed.size() - 1; i >= 0; i--) {
            Map.Entry<BlockPos, BlockState> entry = changed.get(i);
            level.setBlock(entry.getKey(), entry.getValue(), BULK_UPDATE_FLAGS);
        }
        originalBlocks.clear();
    }
}
