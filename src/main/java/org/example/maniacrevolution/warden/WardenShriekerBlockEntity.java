package org.example.maniacrevolution.warden;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.block.entity.ModBlockEntities;
import org.example.maniacrevolution.game.GameManager;
import net.minecraft.nbt.NbtOps;
import net.minecraft.tags.GameEventTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import net.minecraft.world.level.gameevent.*;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

/** Vanilla vibration handling, with a persistent match cooldown and no mob warning tracker. */
public final class WardenShriekerBlockEntity extends BlockEntity implements GameEventListener.Holder<VibrationSystem.Listener>, VibrationSystem {
    private WardenShriekerCycle cycle = new WardenShriekerCycle(0);
    private VibrationSystem.Data vibrationData = new VibrationSystem.Data();
    private final VibrationSystem.User vibrationUser = new VibrationUser();
    private final VibrationSystem.Listener vibrationListener = new VibrationSystem.Listener(this);
    public WardenShriekerBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.WARDEN_SHRIEKER.get(), pos, state); }
    public boolean cooling(long now) { return cycle.cooling(now); }
    public void start(long now) {
        cycle.start(now);
        setChanged();
    }
    public void stopShriek() { cycle.stop(); }
    @Override public VibrationSystem.Data getVibrationData() { return vibrationData; }
    @Override public VibrationSystem.User getVibrationUser() { return vibrationUser; }
    @Override public VibrationSystem.Listener getListener() { return vibrationListener; }
    private final class VibrationUser implements VibrationSystem.User {
        private final PositionSource position = new BlockPositionSource(worldPosition);
        @Override public int getListenerRadius() { return WardenShriekerRules.LISTENER_RADIUS; }
        @Override public PositionSource getPositionSource() { return position; }
        @Override public TagKey<GameEvent> getListenableEvents() { return GameEventTags.SHRIEKER_CAN_LISTEN; }
        @Override public boolean canReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event, GameEvent.Context context) {
            return !getBlockState().getValue(WardenShriekerBlock.SHRIEKING)
                    && SculkShriekerBlockEntity.tryGetPlayer(context.sourceEntity()) != null;
        }
        @Override public void onReceiveVibration(ServerLevel level, BlockPos pos, GameEvent event,
                                                 Entity source, Entity secondarySource, float distance) {
            WardenShriekerManager.step(level, worldPosition, getBlockState(),
                    SculkShriekerBlockEntity.tryGetPlayer(secondarySource != null ? secondarySource : source));
        }
        @Override public void onDataChanged() { setChanged(); }
        @Override public boolean requiresAdjacentChunksToBeTicking() { return true; }
    }
    public static void tick(ServerLevel level, BlockPos pos, BlockState state, WardenShriekerBlockEntity entity) {
        VibrationSystem.Ticker.tick(level, entity.vibrationData, entity.vibrationUser);
        // Vibration delivery may have changed SHRIEKING during this tick.
        state = entity.getBlockState();
        boolean allowed = WardenShriekerData.get(level.getServer()).enabled() && GameManager.getPhaseValue() >= 1
                && GameManager.getPhaseValue() <= 3;
        boolean shriek = allowed && entity.cycle.shrieking(level.getGameTime());
        boolean cooling = entity.cooling(level.getGameTime());
        if (!allowed) entity.stopShriek();
        if (state.getValue(WardenShriekerBlock.SHRIEKING) && !shriek) WardenShriekerManager.stop(level, pos);
        if (state.getValue(WardenShriekerBlock.SHRIEKING) != shriek || state.getValue(WardenShriekerBlock.COOLING) != cooling)
            level.setBlock(pos, state.setValue(WardenShriekerBlock.SHRIEKING, shriek).setValue(WardenShriekerBlock.COOLING, cooling), 3);
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag); cycle = new WardenShriekerCycle(tag.getLong("cooldown_until"));
        vibrationData = tag.contains("listener", 10)
                ? VibrationSystem.Data.CODEC.parse(NbtOps.INSTANCE, tag.getCompound("listener")).result().orElseGet(VibrationSystem.Data::new)
                : new VibrationSystem.Data();
        // Do not restart an interrupted scream after chunk reload or server restart.
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tag.putLong("cooldown_until", cycle.savedCooldown());
        VibrationSystem.Data.CODEC.encodeStart(NbtOps.INSTANCE, vibrationData).result().ifPresent(data -> tag.put("listener", data));
    }
}
