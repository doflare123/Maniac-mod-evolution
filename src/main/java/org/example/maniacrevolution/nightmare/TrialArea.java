package org.example.maniacrevolution.nightmare;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

class TrialArea {
    final ServerLevel level;
    final BlockPos origin;
    final int width;
    final int height;
    final int depth;
    final List<Entity> entities = new ArrayList<>();

    TrialArea(ServerLevel level, BlockPos origin, int width, int height, int depth) {
        this.level = level;
        this.origin = origin;
        this.width = width;
        this.height = height;
        this.depth = depth;
    }

    void destroy() {
        for (Entity entity : entities) {
            if (entity.isAlive()) entity.discard();
        }
        for (int y = height; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
