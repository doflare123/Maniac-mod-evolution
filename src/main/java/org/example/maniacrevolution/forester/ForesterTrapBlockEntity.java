package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.block.entity.ModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class ForesterTrapBlockEntity extends BlockEntity implements GeoBlockEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public ForesterTrapBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FORESTER_TRAP.get(), pos, state); }
    public static void tick(Level level, BlockPos pos, BlockState state, ForesterTrapBlockEntity trap) {
        if (state.getValue(ForesterTrapBlock.STATE) == ForesterTrapBlock.State.CAUGHT) {
            if (!ForesterManager.occupied(level, pos)) level.setBlockAndUpdate(pos, state.setValue(ForesterTrapBlock.STATE, ForesterTrapBlock.State.SPENT));
            return;
        }
        if (state.getValue(ForesterTrapBlock.STATE) != ForesterTrapBlock.State.OPEN) return;
        AABB trigger = new AABB(pos).deflate(0.13, 0, 0.13).setMaxY(pos.getY() + 0.3);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, trigger, ForesterManager::vulnerable)) {
            if (ForesterManager.capture(player, pos)) break;
        }
    }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "jaws", 4, state -> state.setAndContinue(
                RawAnimation.begin().thenLoop(getBlockState().getValue(ForesterTrapBlock.STATE).getSerializedName()))));
    }
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
