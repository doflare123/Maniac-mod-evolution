package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
            if (!player.getMainHandItem().is(ModItems.AWAKENING_NEEDLE.get())) return;
            if (!(level.getBlockEntity(pos) instanceof NightmareCocoonBlockEntity)) return;
            if (!(player instanceof ServerPlayer serverPlayer)) return;

            ModNetworking.sendToPlayer(new OpenCocoonNeedleMinigamePacket(pos.immutable()), serverPlayer);
        }
    }
}
