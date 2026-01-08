package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class HotbarInputHandler {

    private static final int MAX_NORMAL_SLOT = 5; // Последний нормальный слот (0-5)
    private static final int MAX_SLOT = 8; // Последний слот (включая пенальти)

//    @SubscribeEvent
//    public static void onKeyInput(InputEvent.Key event) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null) return;
//
//        int key = event.getKey();
//
//        // Разрешаем клавиши 1-9 для выбора всех слотов (включая пенальти 7-9)
//        // Ничего не блокируем
//    }

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        double scrollDelta = event.getScrollDelta();
        int currentSlot = mc.player.getInventory().selected;

        // Прокрутка вверх (от игрока)
        if (scrollDelta > 0) {
            int newSlot = currentSlot - 1;
            if (newSlot < 0) {
                newSlot = MAX_SLOT; // Циклический переход с 0 на 8 (последний пенальти-слот)
            }

            mc.player.getInventory().selected = newSlot;
            event.setCanceled(true);
        }
        // Прокрутка вниз (к игроку)
        else if (scrollDelta < 0) {
            int newSlot = currentSlot + 1;
            if (newSlot > MAX_SLOT) {
                newSlot = 0; // Циклический переход с 8 на 0
            }

            mc.player.getInventory().selected = newSlot;
            event.setCanceled(true);
        }
    }
}