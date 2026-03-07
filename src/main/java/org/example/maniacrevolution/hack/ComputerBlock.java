package org.example.maniacrevolution.hack;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Блок компьютера.
 * Взаимодействие:
 *   - Creative + ПКМ → открывается GUI настройки computerId
 *   - Adventure + team survivors → запускает взлом
 */
public class ComputerBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Хитбокс чуть меньше блока (компьютер занимает не весь блок)
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 20, 16);

    public ComputerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING,
                ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED; // Рендерит через BlockEntityRenderer
    }

    // ── Взаимодействие ────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(pos) instanceof ComputerBlockEntity be)) return InteractionResult.FAIL;

        // Creative: настройка computerId
        if (player.isCreative()) {
            openSetupScreen(sp, be, pos);
            return InteractionResult.CONSUME;
        }

        // Adventure + survivors: взлом
        if (isSurvivorAdventure(sp)) {
            HackManager.get().onPlayerActivate(sp, pos, be.getComputerId());
            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    private void openSetupScreen(ServerPlayer player, ComputerBlockEntity be, BlockPos pos) {
        // Открываем простое серверное меню для ввода ID
        // Используем chat-команду как альтернативу полноценному GUI
        player.sendSystemMessage(Component.literal(
                "§6[Компьютер] §fТекущий ID: §e" + be.getComputerId() +
                        "\n§7Чтобы изменить: §f/maniacrev computer setid " +
                        pos.getX() + " " + pos.getY() + " " + pos.getZ() + " <id>"));
    }

    private static boolean isSurvivorAdventure(ServerPlayer p) {
        if (p.gameMode.getGameModeForPlayer() !=
                net.minecraft.world.level.GameType.ADVENTURE) return false;
        var team = p.getTeam();
        return team != null && "survivors".equalsIgnoreCase(team.getName());
    }

    // ── BlockEntity ───────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        // Тикер нужен для анимации на клиенте
        if (level.isClientSide()) {
            return createTickerHelper(type,
                    ModHackRegistry.COMPUTER_BLOCK_ENTITY.get(),
                    ComputerBlockEntity::clientTick);
        }
        return null;
    }
}