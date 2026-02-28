package org.example.maniacrevolution.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.renderer.MimicBlockRenderer;
import org.example.maniacrevolution.client.renderer.PlagueOrbRenderer;
import org.example.maniacrevolution.entity.ModEntities;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetupEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MIMIC_BLOCK.get(), MimicBlockRenderer::new);
        event.registerEntityRenderer(ModEntities.PLAGUE_ORB.get(), PlagueOrbRenderer::new);
    }

}
