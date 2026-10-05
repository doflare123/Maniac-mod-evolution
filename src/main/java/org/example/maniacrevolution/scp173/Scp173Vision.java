package org.example.maniacrevolution.scp173;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.AbstractGlassBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class Scp173Vision {
    private Scp173Vision() {}

    public static boolean sees(ServerPlayer observer, ServerPlayer statue) {
        if (observer.level() != statue.level()) return false;
        Vec3 eye = observer.getEyePosition();
        for (Vec3 local : Scp173Geometry.surface()) {
            Vec3 point = Scp173Geometry.worldPoint(local, statue.position(), statue.getYRot(),
                    statue.getSwimAmount(1), statue.getXRot(), statue.isInWater());
            Vec3 delta = point.subtract(eye);
            if (Scp173Rules.inView(delta.x, delta.y, delta.z, observer.getYRot(), observer.getXRot(), Scp173GameplayManager.sightRange(observer))
                    && clear(observer, eye, point)) return true;
        }
        return false;
    }

    static boolean clear(ServerPlayer observer, Vec3 eye, Vec3 point) {
        // Vanilla visual shapes retain holes in slabs, stairs, fences etc. Collision alone is
        // not opacity: explicitly pass glass blocks and panes, and ignore fluids like vanilla sight.
        return BlockGetter.traverseBlocks(eye, point, observer, (player, pos) -> {
            var level = player.level();
            if (!level.hasChunkAt(pos)) return Boolean.FALSE;
            var state = level.getBlockState(pos);
            if (state.getBlock() instanceof AbstractGlassBlock || state.getBlock() instanceof StainedGlassPaneBlock
                    || state.is(Blocks.GLASS_PANE) || state.is(Blocks.TINTED_GLASS)) return null;
            var shape = state.getVisualShape(level, pos, CollisionContext.of(player));
            return shape.clip(eye, point, pos) == null ? null : Boolean.FALSE;
        }, player -> Boolean.TRUE);
    }
}
