package org.example.maniacrevolution.event;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.GameRulesConfig;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class DebugKeyEvents {

    private static int lastWarningTick = 0;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (!GameRulesConfig.isHitboxDebugAllowed()) {
            // Проверяем нажатие F3 и B каждый тик
            long window = mc.getWindow().getWindow();
            boolean f3Down = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_F3) == GLFW.GLFW_PRESS;
            boolean bDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_B) == GLFW.GLFW_PRESS;

            // Если обе клавиши нажаты - принудительно отключаем хитбоксы
            if (f3Down && bDown) {
                try {
                    var dispatcher = mc.getEntityRenderDispatcher();
                    if (dispatcher != null && dispatcher.shouldRenderHitBoxes()) {
                        dispatcher.setRenderHitBoxes(false);

                        // Показываем предупреждение раз в секунду
                        int currentTick = mc.player.tickCount;
                        if (currentTick - lastWarningTick > 20) {
                            mc.player.displayClientMessage(
                                    net.minecraft.network.chat.Component.literal("§cHitbox debug is disabled"),
                                    true
                            );
                            lastWarningTick = currentTick;
                        }
                    }
                } catch (Exception e) {
                    // Игнорируем
                }
            }

            // Принудительно выключаем хитбоксы каждый тик
            try {
                var dispatcher = mc.getEntityRenderDispatcher();
                if (dispatcher != null && dispatcher.shouldRenderHitBoxes()) {
                    dispatcher.setRenderHitBoxes(false);
                }
            } catch (Exception e) {
                // Игнорируем
            }
        }
    }
}