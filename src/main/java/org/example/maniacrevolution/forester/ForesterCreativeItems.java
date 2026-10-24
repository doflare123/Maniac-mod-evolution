package org.example.maniacrevolution.forester;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.*;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ForesterCreativeItems {
    @SubscribeEvent public static void contents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.FORESTER_TRAP); event.accept(ModItems.PEPPER_CANISTER); event.accept(ModItems.FORESTER_FLASHLIGHT);
        }
    }
}
