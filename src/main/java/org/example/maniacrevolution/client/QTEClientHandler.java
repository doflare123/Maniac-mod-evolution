package org.example.maniacrevolution.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.keybind.ModKeybinds;
import org.example.maniacrevolution.network.ModNetworking;
import org.example.maniacrevolution.network.packets.QTEKeyPressPacket;

import java.util.Random;

@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class QTEClientHandler {
    private static boolean isActive = false;
    private static int generatorNumber = 0;
    private static long lastQTETime = 0;
    private static long nextQTEDelay = 0;
    private static QTEState currentQTE = null;
    private static final Random random = new Random();

    public static void startQTE(int generator) {
        isActive = true;
        generatorNumber = generator;
        lastQTETime = System.currentTimeMillis();
        nextQTEDelay = (3 + random.nextInt(5)) * 1000L; // 3-7 секунд
        currentQTE = null;
    }

    public static void stopQTE() {
        isActive = false;
        currentQTE = null;
        generatorNumber = 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();

        // Проверяем, нужно ли создать новый QTE
        if (currentQTE == null && currentTime - lastQTETime >= nextQTEDelay) {
            currentQTE = new QTEState(random.nextInt(4)); // 0-3 для 4 кнопок
            lastQTETime = currentTime;
        }

        // Рендерим активный QTE
        if (currentQTE != null) {
            currentQTE.update();
            currentQTE.render(event.getGuiGraphics());

            // Если QTE завершён (неудача)
            if (currentQTE.isFinished()) {
                currentQTE = null;
                nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!isActive || currentQTE == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return; // Игнорируем если открыто меню

        int pressedKey = -1;

        if (ModKeybinds.QTE_KEY_1.consumeClick()) pressedKey = 0;
        else if (ModKeybinds.QTE_KEY_2.consumeClick()) pressedKey = 1;
        else if (ModKeybinds.QTE_KEY_3.consumeClick()) pressedKey = 2;
        else if (ModKeybinds.QTE_KEY_4.consumeClick()) pressedKey = 3;

        if (pressedKey != -1) {
            boolean success = currentQTE.checkSuccess(pressedKey);

            if (success) {
                // Отправляем пакет на сервер для выполнения функции датапака
                ModNetworking.CHANNEL.sendToServer(new QTEKeyPressPacket(pressedKey, generatorNumber, true));

                // Сбрасываем текущий QTE и планируем следующий
                currentQTE = null;
                nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
            } else {
                // Неправильная кнопка - QTE проваливается
                currentQTE = null;
                nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
            }
        }
    }

    public static boolean isQTEActive() {
        return isActive;
    }
}
