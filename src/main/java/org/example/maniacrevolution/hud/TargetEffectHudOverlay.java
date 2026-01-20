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
 * Показывает предупреждение игроку ОДИН РАЗ когда он становится целью Агента 47
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TargetEffectHudOverlay {

    private static boolean hadEffectLastTick = false;
    private static long effectStartTime = 0;
    private static final long SHOW_DURATION = 10000; // Показывать 10 секунд ОДИН РАЗ

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null) return;

        boolean hasEffect = player.hasEffect(ModEffects.TARGET_EFFECT.get());
        long currentTime = System.currentTimeMillis();

        // Обнаруживаем НОВОЕ наложение эффекта
        if (hasEffect && !hadEffectLastTick) {
            // Эффект только что наложен!
            effectStartTime = currentTime;
            hadEffectLastTick = true;

            // Звук предупреждения
            player.playSound(net.minecraft.sounds.SoundEvents.WITHER_SPAWN, 0.5F, 0.8F);

            System.out.println("[TargetHUD] Effect applied, showing warning");
        } else if (!hasEffect && hadEffectLastTick) {
            // Эффект снят
            hadEffectLastTick = false;
            effectStartTime = 0;
            System.out.println("[TargetHUD] Effect removed");
            return;
        }

        // Показываем HUD только если:
        // 1. Эффект активен
        // 2. Прошло меньше SHOW_DURATION с момента наложения
        if (!hasEffect || effectStartTime == 0) {
            return;
        }

        long timeSinceStart = currentTime - effectStartTime;
        if (timeSinceStart > SHOW_DURATION) {
            // Прошло больше 10 секунд - больше не показываем
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Предупреждающий текст
        String warningText = "§c§l⚠ ВЫ ЦЕЛЬ АГЕНТА 47 ⚠";
        int textWidth = mc.font.width(warningText);
        int textX = (screenWidth - textWidth) / 2;
        int textY = 35;

        // Основной текст
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
    }
}