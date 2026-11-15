package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Emitted light follows the luminous material, rather than giving every block the color of fire. */
public final class AtmosphereLightPalette {
    private AtmosphereLightPalette() {}

    public static int color(BlockState state) {
        if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) return 0xFF2434;
        if (state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)
                || state.is(Blocks.SOUL_LANTERN) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SOUL_FIRE)) return 0x50E5F2;
        if (state.is(Blocks.SEA_LANTERN)) return 0xA0F2E6;
        if (state.is(Blocks.BEACON)) return 0xC4FFF4;
        if (state.is(Blocks.SEA_PICKLE)) return 0xB3E876;
        if (state.is(Blocks.END_ROD)) return 0xE9DCFF;
        if (state.is(Blocks.AMETHYST_CLUSTER) || state.is(Blocks.LARGE_AMETHYST_BUD)
                || state.is(Blocks.MEDIUM_AMETHYST_BUD) || state.is(Blocks.SMALL_AMETHYST_BUD)) return 0xC087FF;
        if (state.is(Blocks.CRYING_OBSIDIAN) || state.is(Blocks.RESPAWN_ANCHOR)
                || state.is(Blocks.NETHER_PORTAL)) return 0xA25AFF;
        if (state.is(Blocks.OCHRE_FROGLIGHT)) return 0xFFE2A0;
        if (state.is(Blocks.VERDANT_FROGLIGHT)) return 0xB6FFB4;
        if (state.is(Blocks.PEARLESCENT_FROGLIGHT)) return 0xF0C5FF;
        if (state.is(Blocks.SHROOMLIGHT)) return 0xFFAD66;
        if (state.is(Blocks.GLOWSTONE) || state.is(Blocks.REDSTONE_LAMP)) return 0xFFDC9C;
        if (state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) return 0xFF7126;
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH) || state.is(Blocks.LANTERN)
                || state.is(Blocks.CAMPFIRE) || state.is(Blocks.FIRE)
                || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER) || state.is(Blocks.JACK_O_LANTERN)) return 0xFFC477;
        return 0xFFF0D8;
    }

    public static Vec3 position(BlockPos block, BlockState state) {
        Vec3 center = Vec3.atCenterOf(block);
        if (state.is(Blocks.WALL_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH)) {
            Direction wall = state.getValue(WallTorchBlock.FACING).getOpposite();
            return center.add(wall.getStepX() * .27, .42, wall.getStepZ() * .27);
        }
        if (state.is(Blocks.TORCH) || state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.SOUL_TORCH))
            return center.add(0, .2, 0);
        return center;
    }
}
