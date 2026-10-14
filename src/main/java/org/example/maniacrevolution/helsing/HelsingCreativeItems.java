package org.example.maniacrevolution.helsing;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class HelsingCreativeItems {
    @SubscribeEvent public static void contents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) return;
        event.accept(ModItems.HELSING_CROSSBOW); event.accept(ModItems.HELSING_HAT);
        for (HelsingBoltItem.Kind kind : HelsingBoltItem.Kind.values()) event.accept(kind.item());
    }
    private HelsingCreativeItems() {}
}
