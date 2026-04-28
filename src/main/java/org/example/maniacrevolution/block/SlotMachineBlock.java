package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.example.maniacrevolution.dodepovich.DodepovichCasinoManager;
import org.example.maniacrevolution.effect.ModEffects;
import org.example.maniacrevolution.item.DodepovichCoinItem;
import org.example.maniacrevolution.sound.ModSounds;
import org.jetbrains.annotations.Nullable;

public class SlotMachineBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF = EnumProperty.create("half", DoubleBlockHalf.class);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public SlotMachineBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1 || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        return defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (direction.getAxis().isVertical()) {
            boolean lowerNeedsUpper = half == DoubleBlockHalf.LOWER && direction == net.minecraft.core.Direction.UP
                    && !neighborState.is(this);
            boolean upperNeedsLower = half == DoubleBlockHalf.UPPER && direction == net.minecraft.core.Direction.DOWN
                    && !neighborState.is(this);
            if (lowerNeedsUpper || upperNeedsLower) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        DoubleBlockHalf half = state.getValue(HALF);
        BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState otherState = level.getBlockState(otherPos);
        if (otherState.is(this)) {
            level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), 35);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof DodepovichCoinItem coinItem)) {
            player.displayClientMessage(Component.literal("§7Автомат принимает только монетки Додеповича."), true);
            return InteractionResult.FAIL;
        }

        if (!DodepovichCasinoManager.isDodepovich(serverPlayer)) {
            player.displayClientMessage(Component.literal("§cТолько Додепович может играть на этом автомате."), true);
            return InteractionResult.FAIL;
        }

        if (serverPlayer.hasEffect(ModEffects.DODEPOVICH_SLOT_COOLDOWN.get())) {
            player.displayClientMessage(Component.literal("§6Казино на перерыве. Подожди перед следующим прокрутом."), true);
            return InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        serverPlayer.addEffect(new MobEffectInstance(ModEffects.DODEPOVICH_SLOT_COOLDOWN.get(), 30 * 20, 0, false, true, true));
        level.playSound(null, pos, ModSounds.SLOT_INSERT.get(), SoundSource.BLOCKS, 0.9f, 1.0f);
        DodepovichCasinoManager.playSlotMachine(serverPlayer, coinItem.getCoin());
        return InteractionResult.SUCCESS;
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }
}
