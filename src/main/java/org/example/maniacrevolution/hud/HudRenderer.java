package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;

/**
 * Рендер кастомных HUD элементов
 * ВАЖНО: Рендерит только если:
 * 1. Игрок НЕ в креативе/наблюдателе
 * 2. Кастомный HUD включен
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class HudRenderer {

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        LevelHud.render(gui, 5, 5);
        PerkHud.render(gui, 5, screenH - 70);
        TimerHud.render(gui, screenW / 2, 5);
    }

    /**
     * Скрываем ванильные элементы HUD когда активен кастомный
     */
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        // В креативе/наблюдателе - показываем стандартный HUD
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        // Кастомный HUD выключен - показываем стандартный HUD
        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        // Кастомный HUD включен И не креатив - СКРЫВАЕМ ванильные элементы
        // Сравниваем напрямую с .type()
        if (event.getOverlay() == VanillaGuiOverlay.HOTBAR.type() ||
                event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type() ||
                event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type() ||
                event.getOverlay() == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {

            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderItemName(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.ITEM_NAME.type()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        event.setCanceled(true);
    }
}