package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.block.entity.NightmareCocoonBlockEntity;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.OpenCocoonNeedleMinigamePacket;
import org.jetbrains.annotations.Nullable;

public class NightmareCocoonBlock extends Block implements EntityBlock {
    public NightmareCocoonBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NightmareCocoonBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !canOpenNeedleMinigame(level, pos, player)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            openNeedleMinigame(serverPlayer, pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean canOpenNeedleMinigame(Level level, BlockPos pos, Player player) {
        return player.getMainHandItem().is(ModItems.AWAKENING_NEEDLE.get())
                && level.getBlockEntity(pos) instanceof NightmareCocoonBlockEntity;
    }

    private static void openNeedleMinigame(ServerPlayer player, BlockPos pos) {
        ModNetworking.sendToPlayer(new OpenCocoonNeedleMinigamePacket(pos.immutable()), player);
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class HitHandler {
        private HitHandler() {}

        @SubscribeEvent
        public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
            Player player = event.getEntity();
            Level level = player.level();
            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof NightmareCocoonBlock)) return;

            event.setCanceled(true);
            if (level.isClientSide()) return;
            if (!canOpenNeedleMinigame(level, pos, player)) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            openNeedleMinigame(serverPlayer, pos);
        }
    }
}
