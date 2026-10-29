package org.example.maniacrevolution.engineer;

import net.minecraft.core.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.example.maniacrevolution.block.entity.ModBlockEntities;

/** Real collision blocks; timed dismantling is independent of vanilla Adventure mining. */
public final class EngineerDeviceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public final boolean barricade;
    public EngineerDeviceBlock(boolean barricade) {
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                .strength(-1, 3600000).sound(SoundType.METAL).noOcclusion().noLootTable());
        this.barricade = barricade;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, HALF); }
    public static BlockPos base(BlockPos pos, BlockState state) { return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos; }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!barricade) return box(3, 0, 4, 13, 13, 12);
        int height = state.getValue(HALF) == DoubleBlockHalf.UPPER ? 8 : 16;
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? box(0, 0, 6, 16, height, 10) : box(6, 0, 0, 10, height, 16);
    }
    @Override public VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The open lattice is not an opaque screen: players can watch SCP-173 over/through it.
        return Shapes.empty();
    }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) return level.getBlockState(pos.below()).is(this);
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, Direction side, BlockState neighbour, LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.SUCCESS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new EngineerDeviceBlockEntity(pos, state) : null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.ENGINEER_DEVICE.get(), EngineerDeviceBlockEntity::tick);
    }
    @Override public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock())) {
            if (!level.isClientSide) EngineerManager.forget(level.dimension().location(), base(pos, state));
            if (barricade) {
                BlockPos other = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos.above();
                if (level.getBlockState(other).is(this)) level.removeBlock(other, false);
            }
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
