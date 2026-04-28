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

    private static boolean hasQuickReflexes = false;

    public static void startQTE(int generator) {
        generatorNumber = generator;
        // Если QTE уже активен и показывается — не сбрасываем его.
        // Просто помечаем как активный, следующий QTE появится по таймеру.
        if (isActive) return;
        isActive = true;
        lastQTETime = System.currentTimeMillis();
        nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
        currentQTE = null;
    }

    public static void stopQTE() {
        isActive = false;
        currentQTE = null;
        generatorNumber = 0;
    }

    public static void setQuickReflexes(boolean hasIt) {
        hasQuickReflexes = hasIt;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!isActive) return;

        long currentTime = System.currentTimeMillis();

        if (currentQTE == null && currentTime - lastQTETime >= nextQTEDelay) {
            currentQTE = new QTEState(random.nextInt(4), hasQuickReflexes);
            lastQTETime = currentTime;
        }

        if (currentQTE != null) {
            currentQTE.update();
            currentQTE.render(event.getGuiGraphics());

            if (currentQTE.isFinished()) {
                System.out.println("QTE timeout - sending FAIL");
                ModNetworking.CHANNEL.sendToServer(
                        new QTEKeyPressPacket(-1, generatorNumber, false));
                currentQTE = null;
                nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
            }
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!isActive || currentQTE == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        // Режим наказания: все 4 кнопки одинаковые — ловим ЛЮБОЕ нажатие
        if (currentQTE.isPunishMode()) {
            // event.getKey() — GLFW keyCode
            int keyCode = event.getKey();
            // Только на нажатие (action=1), не на удержание (action=2)
            if (event.getAction() != 1) return;
            // Игнорируем служебные клавиши (модификаторы)
            if (keyCode == 340 || keyCode == 344 || // Shift
                    keyCode == 341 || keyCode == 345 || // Ctrl
                    keyCode == 342 || keyCode == 346)   // Alt
                return;

            QTEState.QTEHitResult hit = currentQTE.checkHit(keyCode);
            ModNetworking.CHANNEL.sendToServer(
                    new QTEKeyPressPacket(keyCode, generatorNumber, hit.isSuccess(), hit == QTEState.QTEHitResult.CRIT));
            currentQTE = null;
            nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
            return;
        }

        // Обычный режим: проверяем 4 привязанные кнопки
        int pressedKey = -1;
        if (ModKeybinds.QTE_KEY_1.consumeClick()) pressedKey = 0;
        else if (ModKeybinds.QTE_KEY_2.consumeClick()) pressedKey = 1;
        else if (ModKeybinds.QTE_KEY_3.consumeClick()) pressedKey = 2;
        else if (ModKeybinds.QTE_KEY_4.consumeClick()) pressedKey = 3;

        if (pressedKey != -1) {
            QTEState.QTEHitResult hit = currentQTE.checkHit(pressedKey);
            ModNetworking.CHANNEL.sendToServer(
                    new QTEKeyPressPacket(pressedKey, generatorNumber, hit.isSuccess(), hit == QTEState.QTEHitResult.CRIT));
            currentQTE = null;
            nextQTEDelay = (3 + random.nextInt(5)) * 1000L;
        }
    }

    public static boolean isQTEActive()        { return isActive; }
    public static boolean hasQuickReflexesPerk() { return hasQuickReflexes; }
}