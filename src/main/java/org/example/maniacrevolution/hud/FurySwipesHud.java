package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.example.maniacrevolution.client.ClientFurySwipesData;
import org.example.maniacrevolution.fleshheap.ClientFleshHeapData;

/**
 * Иконка стаков Fury Swipes рядом с FleshHeap.
 * Если FleshHeap активен — смещается правее, не перекрывая его.
 *
 * ДОБАВИТЬ в CustomHud.render() СРАЗУ ПОСЛЕ renderFleshHeap(...):
 *   FurySwipesHud.render(guiGraphics, scaledWidth / 2, mainY - 10);
 *
 * Текстура круглой иконки: assets/maniacrev/textures/gui/fury_swipes.png (16x16)
 */
public class FurySwipesHud {

    private static final int ICON_SIZE = 16;
    private static final int GAP = 4;

    public static void render(GuiGraphics gui, int centerX, int centerY) {
        int stacks = ClientFurySwipesData.getSelfStackCount();
        if (stacks <= 0) return;

        Minecraft mc = Minecraft.getInstance();

        // Смещаемся правее если FleshHeap тоже показывается
        boolean fleshActive = ClientFleshHeapData.getStacks() > 0;
        int offsetX = fleshActive ? (ICON_SIZE + GAP) : 0;

        int x = centerX - ICON_SIZE / 2 + offsetX;
        int y = centerY - ICON_SIZE / 2;

        gui.pose().pushPose();

        ResourceLocation texture = new ResourceLocation("maniacrev", "textures/gui/fury_swipes.png");
        try {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.enableBlend();
            gui.blit(texture, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            RenderSystem.disableBlend();
        } catch (Exception ignored) {
            // Пока нет текстуры — оранжевый круг-заглушка
            gui.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0xFFCC4400);
            gui.renderOutline(x, y, ICON_SIZE, ICON_SIZE, 0xFFFF6600);
        }

        // Цифра стаков поверх иконки
        String text = String.valueOf(stacks);
        int textWidth = mc.font.width(text);
        int tx = x + (ICON_SIZE - textWidth) / 2;
        int ty = y + ICON_SIZE / 2 - 4;

        // Обводка
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx != 0 || dy != 0)
                    gui.drawString(mc.font, text, tx + dx, ty + dy, 0xFF000000, false);
            }
        }
        gui.drawString(mc.font, text, tx, ty, 0xFFFF6600, false);

        gui.pose().popPose();
    }
}