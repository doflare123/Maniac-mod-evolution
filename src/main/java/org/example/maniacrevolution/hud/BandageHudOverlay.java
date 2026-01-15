package org.example.maniacrevolution.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.item.BandageItem;
import org.joml.Matrix4f;

/**
 * HUD overlay для отображения прогресса использования бинта
 * Оптимизированная версия с использованием BufferBuilder
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class BandageHudOverlay {

    private static final int CIRCLE_RADIUS = 20;
    private static final int CIRCLE_SEGMENTS = 48;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || !player.isUsingItem()) return;

        ItemStack usingItem = player.getUseItem();
        if (!(usingItem.getItem() instanceof BandageItem)) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Центр экрана
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;

        // Рассчитываем прогресс
        int useTime = player.getUseItemRemainingTicks();
        int maxUseTime = usingItem.getUseDuration();
        float progress = 1.0F - (useTime / (float) maxUseTime);

        // Определяем, кто цель
        boolean healingSelf = !isTargetingPlayer(player);
        float remainingSeconds = useTime / 20.0F;

        // Смещение от центра
        int offsetX = centerX + 50;
        int offsetY = centerY;

        // Рисуем с помощью BufferBuilder (максимально быстро)
        drawCircularProgressOptimized(graphics, offsetX, offsetY, CIRCLE_RADIUS, progress);

        // Текст
        String timeText = String.format("%.1fs", remainingSeconds);
        int textWidth = mc.font.width(timeText);
        graphics.drawString(mc.font, timeText,
                offsetX - textWidth / 2,
                offsetY - 3,
                0xFFFFFF, true);

        // Статус
        String statusText = healingSelf ? "§aЛечение" : "§eПомощь";
        int statusWidth = mc.font.width(statusText);
        graphics.drawString(mc.font, statusText,
                offsetX - statusWidth / 2,
                offsetY - CIRCLE_RADIUS - 12,
                0xFFFFFF, true);
    }

    /**
     * Оптимизированная отрисовка через BufferBuilder (1 draw call)
     */
    private static void drawCircularProgressOptimized(GuiGraphics graphics, int centerX, int centerY,
                                                      int radius, float progress) {
        Matrix4f matrix = graphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();

        bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        // Фоновый круг (серый, полупрозрачный)
        drawCircleToBuffer(bufferBuilder, matrix, centerX, centerY, radius,
                0.2f, 0.2f, 0.2f, 0.5f, CIRCLE_SEGMENTS);

        // Завершаем отрисовку фона
        BufferUploader.drawWithShader(bufferBuilder.end());

        // Прогресс (зеленый)
        if (progress > 0) {
            bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            int progressSegments = (int) (CIRCLE_SEGMENTS * progress);
            drawArcToBuffer(bufferBuilder, matrix, centerX, centerY, radius - 2,
                    0.0f, 1.0f, 0.0f, 1.0f, progressSegments);
            BufferUploader.drawWithShader(bufferBuilder.end());
        }

        RenderSystem.disableBlend();
    }

    /**
     * Добавляет круг в буфер
     */
    private static void drawCircleToBuffer(BufferBuilder buffer, Matrix4f matrix,
                                           int centerX, int centerY, int radius,
                                           float r, float g, float b, float a,
                                           int segments) {
        for (int i = 0; i <= segments; i++) {
            double angle = 2 * Math.PI * i / segments;
            float x = centerX + (float) (Math.cos(angle) * radius);
            float y = centerY + (float) (Math.sin(angle) * radius);
            float xInner = centerX + (float) (Math.cos(angle) * (radius - 2));
            float yInner = centerY + (float) (Math.sin(angle) * (radius - 2));

            buffer.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, xInner, yInner, 0).color(r, g, b, a).endVertex();
        }
    }

    /**
     * Добавляет дугу (arc) в буфер для прогресса
     */
    private static void drawArcToBuffer(BufferBuilder buffer, Matrix4f matrix,
                                        int centerX, int centerY, int radius,
                                        float r, float g, float b, float a,
                                        int segments) {
        for (int i = 0; i <= segments; i++) {
            // Начинаем с верхней точки (-90 градусов)
            double angle = -Math.PI / 2 + 2 * Math.PI * i / CIRCLE_SEGMENTS;
            float x = centerX + (float) (Math.cos(angle) * radius);
            float y = centerY + (float) (Math.sin(angle) * radius);
            float xInner = centerX + (float) (Math.cos(angle) * (radius - 3));
            float yInner = centerY + (float) (Math.sin(angle) * (radius - 3));

            buffer.vertex(matrix, x, y, 0).color(r, g, b, a).endVertex();
            buffer.vertex(matrix, xInner, yInner, 0).color(r, g, b, a).endVertex();
        }
    }

    /**
     * Проверяет, нацелен ли игрок на другого игрока
     */
    private static boolean isTargetingPlayer(Player player) {
        Minecraft mc = Minecraft.getInstance();
        var hitResult = mc.hitResult;

        if (hitResult == null) return false;

        return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY &&
                ((net.minecraft.world.phys.EntityHitResult) hitResult).getEntity() instanceof Player;
    }
}