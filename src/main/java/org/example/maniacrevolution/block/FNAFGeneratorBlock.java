package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.example.maniacrevolution.block.entity.FNAFGeneratorBlockEntity;
import org.jetbrains.annotations.Nullable;

public class FNAFGeneratorBlock extends Block implements EntityBlock {

    public FNAFGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            BlockEntity be = level.getBlockEntity(pos);

            if (be instanceof FNAFGeneratorBlockEntity generator) {

                // Проверяем команду игрока
                String teamName = player.getScoreboardName();
                boolean isSurvivor = player.getTeam() != null && player.getTeam().getName().equals("survivors");
                boolean isManiac = player.getTeam() != null && player.getTeam().getName().equals("maniac");
                net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
                net.minecraft.world.scores.Objective classObjective = scoreboard.getObjective("ManiacClass");

                if (classObjective == null) {
                    return InteractionResult.FAIL; // Нет scoreboard - не некромант
                }

                net.minecraft.world.scores.Score classScore = scoreboard.getOrCreatePlayerScore(player.getScoreboardName(), classObjective);

                // Маньяк с классом 12 может только ВЫКЛЮЧИТЬ
                if (isManiac && classScore.getScore() == 12) {
                    if (generator.isPowered()) {
                        generator.setPowered(false);
                        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 0.8f);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cГенератор выключен!"), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cГенератор уже выключен!"), true);
                        return InteractionResult.FAIL;
                    }
                }

                // Выживший может только ВКЛЮЧИТЬ
                if (isSurvivor) {
                    if (!generator.isPowered()) {
                        generator.setPowered(true);
                        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 1.0f, 1.2f);
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aГенератор включен!"), true);
                        return InteractionResult.SUCCESS;
                    } else {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aГенератор уже работает!"), true);
                        return InteractionResult.FAIL;
                    }
                }

                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§7Вы не можете использовать генератор"), true);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FNAFGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (lvl, pos, st, be) -> {
            if (be instanceof FNAFGeneratorBlockEntity generator) {
                generator.serverTick((ServerLevel) lvl, pos, st);
            }
        };
    }
}