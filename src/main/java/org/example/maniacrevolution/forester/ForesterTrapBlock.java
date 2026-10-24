package org.example.maniacrevolution.forester;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import org.example.maniacrevolution.block.entity.ModBlockEntities;

public final class ForesterTrapBlock extends BaseEntityBlock {
    public enum State implements StringRepresentable {
        OPEN, CAUGHT, SPENT;
        @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final EnumProperty<State> STATE = EnumProperty.create("state", State.class);
    private static final VoxelShape BASE = Block.box(1, 0, 1, 15, 2, 15);
    public ForesterTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATE, State.OPEN));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(STATE); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(STATE) == State.OPEN ? BASE : Block.box(1, 0, 1, 15, 7, 15);
    }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return BASE; }
    @Override public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
    }
    @Override public BlockState updateShape(BlockState state, net.minecraft.core.Direction direction, BlockState neighbor,
                                           LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }
    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                           InteractionHand hand, BlockHitResult hit) {
        // Work starts once per RMB press through the validated forester input packet.
        return InteractionResult.SUCCESS;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ForesterTrapBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.FORESTER_TRAP.get(), ForesterTrapBlockEntity::tick);
    }
}
