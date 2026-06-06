package org.example.maniacrevolution.client.renderer;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.data.ClientKeeperFormData;

public final class KeeperNightmareRenderEvents {
    private static KeeperNightmareRenderer renderer;

    private KeeperNightmareRenderEvents() {}

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {}

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            renderer = new KeeperNightmareRenderer(event.getContext());
        }
    }

    @Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
            if (renderer == null) return;
            if (!(event.getEntity() instanceof AbstractClientPlayer player)) return;
            if (!ClientKeeperFormData.isKeeper(player)) return;

            event.setCanceled(true);
            renderer.render(player, player.getYRot(), event.getPartialTick(),
                    event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
        }
    }
}
