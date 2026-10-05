package org.example.maniacrevolution.warden.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

final class WardenVisionClassification {
    static final double SURVIVOR_BLUE_RANGE = 30;
    private WardenVisionClassification() {}

    static boolean isInteractive(BlockState state, Level level, BlockPos pos) {
        // Never invoke use(): querying colour must not open menus or change blocks.
        return org.example.maniacrevolution.warden.WardenInteractionPolicy.interactive(state, level, pos);
    }

    static boolean hasInteraction(Class<?> blockClass) {
        return org.example.maniacrevolution.warden.WardenInteractionPolicy.hasInteraction(blockClass);
    }

    enum CreatureTint {
        WHITE(1, 1, 1), GRAY(0.6f, 0.6f, 0.6f), BLUE(0.15f, 0.45f, 1), RED(1, 0.15f, 0.15f);

        final float r, g, b;
        CreatureTint(float r, float g, float b) { this.r = r; this.g = g; this.b = b; }
    }

    static boolean moving(double dx, double dy, double dz) {
        // Client tick displacement, rather than input, animation or residual velocity.
        return dx * dx + dy * dy + dz * dz > 1.0e-6;
    }

    static CreatureTint creatureTint(boolean player, String team, boolean moving, boolean shift, double distanceSquared) {
        if (player && (!moving || shift)) return CreatureTint.WHITE;
        if (player && team != null) {
            if ("survivors".equalsIgnoreCase(team)) return distanceSquared <= SURVIVOR_BLUE_RANGE * SURVIVOR_BLUE_RANGE
                    ? CreatureTint.BLUE : CreatureTint.WHITE;
            if ("maniac".equalsIgnoreCase(team)) return CreatureTint.RED;
        }
        return CreatureTint.GRAY;
    }
}
