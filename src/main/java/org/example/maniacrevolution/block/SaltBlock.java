package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.example.maniacrevolution.util.SaltTracker;

public class SaltBlock extends Block {
    protected static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public SaltBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity livingEntity) {
            // Накладываем эффекты на игрока
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255)); // Максимальное замедление на 2 секунды
            livingEntity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0)); // Слепота на 6 секунд

            // Звук тушения лавы
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

            // Частицы дыма (на клиенте)
            if (level instanceof ServerLevel serverLevel) {
                // Создаем частицы дыма
                for (int i = 0; i < 20; i++) {
                    double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
                    double offsetY = level.random.nextDouble() * 0.3;
                    double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

                    serverLevel.sendParticles(
                            net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            pos.getX() + 0.5 + offsetX,
                            pos.getY() + 0.1 + offsetY,
                            pos.getZ() + 0.5 + offsetZ,
                            1,
                            0.0, 0.05, 0.0,
                            0.01
                    );
                }

                // Убираем позицию из трекера
                SaltTracker tracker = SaltTracker.get(serverLevel);
                tracker.removeSaltPosition(pos);
            }

            // Удаляем блок соли
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Если блок был уничтожен (не только заменен), удаляем из трекера
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            if (level instanceof ServerLevel serverLevel) {
                SaltTracker tracker = SaltTracker.get(serverLevel);
                tracker.removeSaltPosition(pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        return level.getBlockState(belowPos).isFaceSturdy(level, belowPos, Direction.UP);
    }
}