package org.example.maniacrevolution.ghost;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.effect.ModEffects;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class GhostClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        GhostPossessionClientState.tick(mc);

        if (!GhostPossessionClientState.isVictimControlled() || mc.player == null) {
            return;
        }

        mc.options.keyUp.setDown(false);
        mc.options.keyDown.setDown(false);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);
        mc.options.keyJump.setDown(false);
        mc.options.keyShift.setDown(false);
        mc.options.keySprint.setDown(false);
        mc.options.keyAttack.setDown(false);
        mc.options.keyUse.setDown(false);
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        GhostPossessionClientState.clear();
        GhostVisibilityClientState.clear();
    }

    @SubscribeEvent
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!GhostPossessionClientState.isControllerActive()) {
            return;
        }
        // Полный прокси-контроль через отдельные пакеты отключён.
        // Пока используем более стабильную модель:
        // сам Призрак выполняет действия, а тело жертвы только повторяет движение/анимацию.
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.FULL_INVISIBILITY.get())
                || GhostVisibilityClientState.isHidden(event.getEntity().getUUID())) {
            event.setCanceled(true);
            return;
        }

        if (!GhostPossessionClientState.isControllerActive()) {
            return;
        }

        if (event.getEntity().getId() == GhostPossessionClientState.getTargetEntityId()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.hasEffect(ModEffects.FULL_INVISIBILITY.get())) {
            event.setCanceled(true);
        }
    }
}
