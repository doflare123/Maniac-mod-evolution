//package org.example.maniacrevolution.event;
//
//import net.minecraft.client.KeyMapping;
//import net.minecraft.client.Minecraft;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.client.event.InputEvent;
//import net.minecraftforge.eventbus.api.EventPriority;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//import org.example.maniacrevolution.Maniacrev;
//import org.lwjgl.glfw.GLFW;
//
//@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
//public class HotbarKeyEvents {
//
//    @SubscribeEvent(priority = EventPriority.HIGHEST)
//    public static void onKeyInput(InputEvent.Key event) {
//        Minecraft mc = Minecraft.getInstance();
//        if (mc.player == null) return;
//
//        int key = event.getKey();
//        int action = event.getAction();
//
//        // Блокируем клавиши 7, 8, 9 для хотбара
//        if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT) {
//            // Проверяем нажатие клавиш 7, 8, 9
//            if (key == GLFW.GLFW_KEY_7 || key == GLFW.GLFW_KEY_8 || key == GLFW.GLFW_KEY_9) {
//                // Проверяем что это именно для хотбара (не в чате или другом GUI)
//                if (mc.screen == null) {
//                    event.setCanceled(true);
//                }
//            }
//        }
//    }
//}