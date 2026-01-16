package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.effect.ModEffects;

/**
 * HUD оверлей для отображения статуса "Цель"
 * Показывает предупреждение игроку, что он является целью Агента 47
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TargetEffectHudOverlay {

    private static long lastPulse = 0;
    private static final long PULSE_INTERVAL = 1000; // Пульсация каждую секунду

    // Настройки показа HUD
    private static final long SHOW_DURATION = 5000; // Показывать 5 секунд
    private static final long SHOW_INTERVAL = 15000; // Каждые 15 секунд
    private static long lastShowTime = 0;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        // Проверяем, есть ли эффект "Цель"
        if (!player.hasEffect(ModEffects.TARGET_EFFECT.get())) {
            lastShowTime = 0; // Сбрасываем при отсутствии эффекта
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Инициализируем время первого показа
        if (lastShowTime == 0) {
            lastShowTime = currentTime;
        }

        // Вычисляем, нужно ли показывать HUD сейчас
        long timeSinceLastShow = currentTime - lastShowTime;
        long timeInCycle = timeSinceLastShow % SHOW_INTERVAL;

        // Показываем только первые SHOW_DURATION миллисекунд из каждого цикла
        if (timeInCycle > SHOW_DURATION) {
            return; // Не показываем HUD
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Вычисляем альфа-канал для пульсации
        float pulse = (float) Math.sin((currentTime % 2000) / 2000.0 * Math.PI * 2) * 0.3F + 0.7F;

        // Рамка экрана (красная)
        int alpha = (int) (pulse * 150);
        int color = (alpha << 24) | 0xFF0000;

        int borderWidth = 3;

        // Верхняя граница
        graphics.fill(0, 0, screenWidth, borderWidth, color);
        // Нижняя граница
        graphics.fill(0, screenHeight - borderWidth, screenWidth, screenHeight, color);
        // Левая граница
        graphics.fill(0, 0, borderWidth, screenHeight, color);
        // Правая граница
        graphics.fill(screenWidth - borderWidth, 0, screenWidth, screenHeight, color);

        // Предупреждающий текст
        String warningText = "§c§l⚠ ВЫ ЦЕЛЬ АГЕНТА 47 ⚠";
        int textWidth = mc.font.width(warningText);
        int textX = (screenWidth - textWidth) / 2;
        int textY = 10;

        // Основной текст (без тени, чтобы не двоился)
        graphics.drawString(mc.font, warningText, textX, textY, 0xFFFF0000, false);

        // Дополнительная информация
        String infoText = "§7Агент охотится за вами!";
        int infoWidth = mc.font.width(infoText);
        int infoX = (screenWidth - infoWidth) / 2;
        int infoY = textY + 15;

        graphics.drawString(mc.font, infoText, infoX, infoY, 0xFFAAAAAA, false);

        // Иконка черепа (если игрок в опасности)
        if (player.getHealth() < player.getMaxHealth() * 0.3F) {
            String skullIcon = "§c☠";
            int skullWidth = mc.font.width(skullIcon);

            // Анимация мигания
            if (currentTime % 1000 < 500) {
                graphics.drawString(mc.font, skullIcon,
                        screenWidth / 2 - skullWidth / 2,
                        screenHeight - 50,
                        0xFFFF0000, false);
            }
        }

        // Звуковое предупреждение каждую секунду (только когда HUD видим)
        if (currentTime - lastPulse >= PULSE_INTERVAL) {
            lastPulse = currentTime;
            player.playSound(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASS.value(), 0.3F, 0.5F);
        }
    }
}