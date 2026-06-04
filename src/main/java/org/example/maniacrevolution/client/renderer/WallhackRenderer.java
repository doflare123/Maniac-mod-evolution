package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Matrix4f;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Клиентский рендерер для подсветки маньяков через стены.
 * Версия с полупрозрачной заливкой.
 */
@OnlyIn(Dist.CLIENT)
public class WallhackRenderer {

    private static Set<UUID> highlightedPlayers = new HashSet<>();
    private static int remainingTicks = 0;

    public static void setHighlightedPlayers(Set<UUID> players, int durationTicks) {
        highlightedPlayers = new HashSet<>(players);
        remainingTicks = durationTicks;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (remainingTicks > 0) {
            remainingTicks--;

            if (remainingTicks <= 0) {
                highlightedPlayers.clear();
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (highlightedPlayers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        // ОТЛАДКА
        System.out.println("Rendering " + highlightedPlayers.size() + " highlighted players");

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Настраиваем рендер для прозрачности и рендера через стены
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // Рендерим через стены
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableCull(); // Отключаем отсечение задних граней

        int renderedCount = 0;
        for (UUID uuid : highlightedPlayers) {
            Entity entity = mc.level.getPlayerByUUID(uuid);

            // ОТЛАДКА
            System.out.println("  UUID: " + uuid + " -> Entity: " + (entity != null ? entity.getName().getString() : "NULL"));

            if (entity instanceof Player player) {
                renderPlayerHighlight(poseStack, player);
                renderedCount++;
            }
        }

        System.out.println("Actually rendered: " + renderedCount + " players");

        // Восстанавливаем настройки
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static void renderPlayerHighlight(PoseStack poseStack, Player player) {
        AABB aabb = player.getBoundingBox();

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();
        Matrix4f matrix = poseStack.last().pose();

        // Рендерим заливку (красная полупрозрачная)
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawFilledBox(buffer, matrix, aabb, 1.0f, 0.0f, 0.0f, 0.3f);
        tesselator.end();

        // Рендерим обводку (красная яркая)
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        drawBoxOutline(buffer, matrix, aabb, 1.0f, 0.0f, 0.0f, 0.8f);
        tesselator.end();
    }

    /**
     * Рисует заполненный куб.
     */
    private static void drawFilledBox(BufferBuilder buffer, Matrix4f matrix, AABB aabb,
                                      float r, float g, float b, float a) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        // Нижняя грань (Y-)
        quad(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);

        // Верхняя грань (Y+)
        quad(buffer, matrix, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);

        // Северная грань (Z-)
        quad(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);

        // Южная грань (Z+)
        quad(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);

        // Западная грань (X-)
        quad(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);

        // Восточная грань (X+)
        quad(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
    }

    /**
     * Рисует обводку куба.
     */
    private static void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, AABB aabb,
                                       float r, float g, float b, float a) {
        float minX = (float) aabb.minX;
        float minY = (float) aabb.minY;
        float minZ = (float) aabb.minZ;
        float maxX = (float) aabb.maxX;
        float maxY = (float) aabb.maxY;
        float maxZ = (float) aabb.maxZ;

        // Нижняя грань
        line(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Верхняя грань
        line(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Вертикальные рёбра
        line(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        line(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        line(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    /**
     * Рисует квад (4 вершины).
     */
    private static void quad(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x3, y3, z3).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    /**
     * Рисует линию.
     */
    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}