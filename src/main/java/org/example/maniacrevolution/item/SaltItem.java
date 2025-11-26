package org.example.maniacrevolution.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.example.maniacrevolution.block.ModBlocks;
import org.example.maniacrevolution.util.SaltTracker;

public class SaltItem extends Item {
    public SaltItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Direction clickedFace = context.getClickedFace();
        BlockState clickedState = level.getBlockState(clickedPos);

        // Определяем позицию, куда будет поставлена соль
        BlockPos placePos;

        // Если кликнули по верхней грани блока, ставим соль прямо на него
        if (clickedFace == Direction.UP && !clickedState.isAir()) {
            placePos = clickedPos.above();
        } else if (clickedState.isAir() || clickedState.canBeReplaced()) {
            // Если блок воздух или может быть заменен
            placePos = clickedPos;
        } else {
            // В остальных случаях ставим относительно клика
            placePos = clickedPos.relative(clickedFace);
        }

        // Проверяем, можно ли поставить соль на эту позицию
        BlockState stateAtPlace = level.getBlockState(placePos);
        if (!stateAtPlace.isAir() && !stateAtPlace.canBeReplaced()) {
            return InteractionResult.FAIL;
        }

        // Проверяем, есть ли под солью твердый блок
        BlockPos belowPos = placePos.below();
        BlockState belowState = level.getBlockState(belowPos);
        if (!belowState.isFaceSturdy(level, belowPos, Direction.UP)) {
            return InteractionResult.FAIL;
        }

        // Ставим соль
        level.setBlock(placePos, ModBlocks.SALT_BLOCK.get().defaultBlockState(), 3);

        // Звук размещения
        level.playSound(null, placePos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        // Добавляем позицию в трекер
        if (level instanceof ServerLevel serverLevel) {
            SaltTracker tracker = SaltTracker.get(serverLevel);
            tracker.addSaltPosition(placePos);
        }

        // Уменьшаем количество предметов
        if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
            context.getItemInHand().shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}