package org.example.maniacrevolution.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class HotbarScrollEvents {

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Player player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;

        double scrollDelta = event.getScrollDelta();
        if (scrollDelta == 0) return;

        int currentSlot = player.getInventory().selected;
        int newSlot = currentSlot;

        if (scrollDelta > 0) {
            // Скролл вверх (к меньшим номерам)
            newSlot--;
            if (newSlot < 0) {
                newSlot = 5; // Возврат на слот 6 (индекс 5)
            }
        } else {
            // Скролл вниз (к большим номерам)
            newSlot++;
            if (newSlot > 5) {
                newSlot = 0; // Возврат на слот 1 (индекс 0)
            }
        }

        player.getInventory().selected = newSlot;
        event.setCanceled(true); // Отменяем стандартное поведение
    }
}