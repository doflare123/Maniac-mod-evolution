package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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
 */
@OnlyIn(Dist.CLIENT)
public class WallhackRenderer {

    // Игроки, которые должны быть подсвечены
    private static Set<UUID> highlightedPlayers = new HashSet<>();
    private static int remainingTicks = 0;

    /**
     * Устанавливает список игроков для подсветки.
     * Вызывается из пакета.
     */
    public static void setHighlightedPlayers(Set<UUID> players, int durationTicks) {
        highlightedPlayers = new HashSet<>(players);
        remainingTicks = durationTicks;
    }

    /**
     * Тик для отсчёта времени.
     */
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

    /**
     * Рендерит подсветку игроков.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (highlightedPlayers.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        poseStack.pushPose();

        // Смещаем на позицию камеры
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Рендерим каждого подсвеченного игрока
        for (UUID uuid : highlightedPlayers) {
            Entity entity = mc.level.getPlayerByUUID(uuid);
            if (entity instanceof Player player) {
                renderPlayerOutline(poseStack, player);
            }
        }

        poseStack.popPose();
    }

    /**
     * Рендерит обводку игрока.
     */
    private static void renderPlayerOutline(PoseStack poseStack, Player player) {
        // Получаем хитбокс игрока
        AABB aabb = player.getBoundingBox();

        // Цвет: красный с прозрачностью
        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.5f;

        // Настраиваем рендер
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // КРИТИЧНО: рендерим через стены
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        Matrix4f matrix = poseStack.last().pose();

        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        // Рисуем куб вокруг игрока
        drawBox(buffer, matrix, aabb, red, green, blue, alpha);

        tesselator.end();

        // Восстанавливаем настройки
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Рисует линии куба.
     */
    private static void drawBox(BufferBuilder buffer, Matrix4f matrix, AABB aabb,
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
     * Рисует одну линию.
     */
    private static void line(BufferBuilder buffer, Matrix4f matrix,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float r, float g, float b, float a) {
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).endVertex();
    }
}