package org.example.maniacrevolution.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.item.Agent47TabletItem;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.Agent47HeldTabletDataPacket;
import org.example.maniacrevolution.system.Agent47TargetManager;

@Mod.EventBusSubscriber
public final class Agent47TabletSyncHandler {
    private static final int SYNC_INTERVAL_TICKS = 10;

    private Agent47TabletSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer agent)) {
            return;
        }
        if (agent.tickCount % SYNC_INTERVAL_TICKS != 0 || !isHoldingTablet(agent)) {
            return;
        }

        ServerPlayer target = Agent47TargetManager.getCurrentTarget(agent);
        int healthPercent = 0;
        if (target != null && target.getMaxHealth() > 0.0F) {
            healthPercent = Math.max(0, Math.min(100,
                    Math.round(target.getHealth() * 100.0F / target.getMaxHealth())));
        }

        ModNetworking.sendToPlayer(new Agent47HeldTabletDataPacket(
                target == null ? null : target.getUUID(), healthPercent), agent);
    }

    private static boolean isHoldingTablet(ServerPlayer player) {
        return player.getMainHandItem().getItem() instanceof Agent47TabletItem
                || player.getOffhandItem().getItem() instanceof Agent47TabletItem;
    }
}
