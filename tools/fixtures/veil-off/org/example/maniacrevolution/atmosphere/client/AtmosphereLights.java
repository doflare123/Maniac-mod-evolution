package org.example.maniacrevolution.atmosphere.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Keeps the common block-change hook linkable without the Veil-dependent light uploader. */
public final class AtmosphereLights {
    public static void blockChanged(BlockPos block, BlockState before, BlockState after) {}
}
