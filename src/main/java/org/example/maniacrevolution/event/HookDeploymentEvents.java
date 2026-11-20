package org.example.maniacrevolution.event;

import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.entity.HookDeployment;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class HookDeploymentEvents {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeployedHookAttack(AttackEntityEvent event) {
        if (HookDeployment.hasActiveHook(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventDeployedHookBlockHit(PlayerInteractEvent.LeftClickBlock event) {
        if (HookDeployment.hasActiveHook(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void clearDeployedHooksOnStop(ServerStoppedEvent event) {
        HookDeployment.clearAll();
    }
}
