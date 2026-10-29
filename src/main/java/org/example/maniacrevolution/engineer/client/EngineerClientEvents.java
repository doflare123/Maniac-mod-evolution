package org.example.maniacrevolution.engineer.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/** Only devices have a custom renderer; the survivor uses the player's normal skin and arms. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class EngineerClientEvents {
    @SubscribeEvent public static void blocks(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(org.example.maniacrevolution.block.entity.ModBlockEntities.ENGINEER_DEVICE.get(), context -> new EngineerDeviceRenderer());
    }
    @SubscribeEvent public static void creative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            e.accept(org.example.maniacrevolution.ModItems.ENGINEER_TABLET); e.accept(org.example.maniacrevolution.ModItems.ENGINEER_REMOTE);
            e.accept(org.example.maniacrevolution.ModItems.ENGINEER_BARRICADE); e.accept(org.example.maniacrevolution.ModItems.ENGINEER_PANEL); e.accept(org.example.maniacrevolution.ModItems.ENGINEER_SCRAP);
        }
    }
}
