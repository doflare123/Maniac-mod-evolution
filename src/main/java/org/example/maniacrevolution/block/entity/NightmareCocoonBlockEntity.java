package org.example.maniacrevolution.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class NightmareCocoonBlockEntity extends BlockEntity implements GeoBlockEntity {
    private static final String HITS_TAG = "Hits";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int hits;

    public NightmareCocoonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NIGHTMARE_COCOON.get(), pos, state);
    }

    public int hit() {
        hits = Math.min(NightmareConfig.COCOON_REQUIRED_HITS, hits + 1);
        sync();
        return hits;
    }

    public float getHealthProgress() {
        return 1.0F - Math.min(1.0F, (float) hits / NightmareConfig.COCOON_REQUIRED_HITS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(HITS_TAG, hits);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hits = tag.getInt(HITS_TAG);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "cocoon", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    private void sync() {
        setChanged();
        Level level = getLevel();
        if (level != null) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
