package org.example.maniacrevolution.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.config.HudConfig;
import org.example.maniacrevolution.util.PlayerModeUtil;

/**
 * Рендер кастомных HUD элементов
 * ВАЖНО: Рендерит только если:
 * 1. Игрок НЕ в креативе/наблюдателе
 * 2. Кастомный HUD включен
 */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public class HudRenderer {
    private static final int ACTIONBAR_GUI_HEIGHT = 86;
    private static final int CHAT_ACTIONBAR_OFFSET = 14;
    private static int savedLeftHeight;
    private static int savedRightHeight;
    private static boolean actionbarHeightAdjusted;

    /**
     * Скрываем ванильные элементы HUD когда активен кастомный
     */
    @SubscribeEvent
    public static void onRenderOverlayPre(RenderGuiOverlayEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null) return;

        // В креативе/наблюдателе - показываем стандартный HUD
        if (!PlayerModeUtil.isSurvivalOrAdventure(mc.player)) {
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

        if (!PlayerModeUtil.isSurvivalOrAdventure(mc.player)) {
            return;
        }

        if (!HudConfig.isCustomHudEnabled()) {
            return;
        }

        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onActionbarPre(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.RECORD_OVERLAY.type()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !PlayerModeUtil.isSurvivalOrAdventure(mc.player)
                || !HudConfig.isCustomHudEnabled() || !(mc.gui instanceof ForgeGui forgeGui)) {
            return;
        }

        savedLeftHeight = forgeGui.leftHeight;
        savedRightHeight = forgeGui.rightHeight;
        int targetHeight = ACTIONBAR_GUI_HEIGHT
                + (mc.screen instanceof ChatScreen ? CHAT_ACTIONBAR_OFFSET : 0);
        forgeGui.leftHeight = Math.max(forgeGui.leftHeight, targetHeight);
        forgeGui.rightHeight = Math.max(forgeGui.rightHeight, targetHeight);
        actionbarHeightAdjusted = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onActionbarPost(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.RECORD_OVERLAY.type()
                || !actionbarHeightAdjusted) {
            return;
        }

        if (Minecraft.getInstance().gui instanceof ForgeGui forgeGui) {
            forgeGui.leftHeight = savedLeftHeight;
            forgeGui.rightHeight = savedRightHeight;
        }
        actionbarHeightAdjusted = false;
    }
}
