package org.example.maniacrevolution.engineer;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.example.maniacrevolution.block.entity.ModBlockEntities;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class EngineerDeviceBlockEntity extends BlockEntity implements GeoBlockEntity {
    UUID owner, match;
    long expires = Long.MAX_VALUE, playingUntil, readyAt;
    int uses = EngineerRules.PANEL_USES, scenario;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public EngineerDeviceBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.ENGINEER_DEVICE.get(), pos, state); }
    public boolean barricade() { return ((EngineerDeviceBlock) getBlockState().getBlock()).barricade; }
    public boolean playing() { return level != null && playingUntil > level.getGameTime(); }
    public int uses() { return uses; }
    public long playingUntil() { return playingUntil; }
    public void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    @Override protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (owner != null) t.putUUID("Owner", owner); if (match != null) t.putUUID("Match", match);
        t.putLong("Expires", expires); t.putLong("PlayingUntil", playingUntil); t.putLong("ReadyAt", readyAt); t.putInt("Uses", uses); t.putInt("Scenario", scenario);
    }
    @Override public void load(CompoundTag t) {
        super.load(t); owner = t.hasUUID("Owner") ? t.getUUID("Owner") : null; match = t.hasUUID("Match") ? t.getUUID("Match") : null;
        expires = t.getLong("Expires"); playingUntil = t.getLong("PlayingUntil"); readyAt = t.getLong("ReadyAt");
        uses = Math.max(0, Math.min(EngineerRules.PANEL_USES, t.getInt("Uses"))); scenario = Math.max(0, Math.min(1, t.getInt("Scenario")));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public AABB getRenderBoundingBox() { return new AABB(worldPosition).inflate(.1, 1, .1); }
    public static void tick(Level level, BlockPos pos, BlockState state, EngineerDeviceBlockEntity be) { EngineerManager.deviceTick(be); }
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }
}
