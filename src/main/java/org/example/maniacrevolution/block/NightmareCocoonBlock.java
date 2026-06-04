package org.example.maniacrevolution.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.nightmare.NightmareConfig;
import org.example.maniacrevolution.nightmare.NightmareManager;

import java.util.HashMap;
import java.util.Map;

public class NightmareCocoonBlock extends Block {
    public NightmareCocoonBlock(Properties properties) {
        super(properties);
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class HitHandler {
        private static final Map<BlockPos, Integer> HITS = new HashMap<>();

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

            if (!player.getMainHandItem().is(ModItems.AWAKENING_NEEDLE.get())) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Нужна Игла пробуждения"), true);
                return;
            }

            int hits = HITS.merge(pos.immutable(), 1, Integer::sum);
            if (hits >= NightmareConfig.COCOON_REQUIRED_HITS && player instanceof ServerPlayer serverPlayer) {
                HITS.remove(pos);
                NightmareManager.getInstance().onCocoonHit(serverPlayer, pos);
            } else {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Кокон трескается: " + hits + "/" + NightmareConfig.COCOON_REQUIRED_HITS), true);
            }
        }
    }
}
