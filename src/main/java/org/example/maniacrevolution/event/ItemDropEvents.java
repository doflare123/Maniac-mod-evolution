package org.example.maniacrevolution.event;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.GameRulesConfig;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public class ItemDropEvents {

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (!GameRulesConfig.isItemDropAllowed()) {
            if (event.getPlayer().isCreative() || event.getPlayer().isSpectator()) {
                return;
            }

            // Отменяем дроп
            event.setCanceled(true);

            // Возвращаем предмет в инвентарь
            ItemEntity itemEntity = event.getEntity();
            ItemStack stack = itemEntity.getItem();

            // Добавляем обратно в инвентарь
            if (!event.getPlayer().getInventory().add(stack)) {
                // Если не получилось - ставим на место откуда взяли
                event.getPlayer().containerMenu.setCarried(stack);
            }
        }
    }
}