package org.example.maniacrevolution.cosmetic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.cosmetic.CosmeticData;
import org.example.maniacrevolution.cosmetic.client.ClientCosmeticCache;
import org.example.maniacrevolution.data.ClientPlayerData;
import org.joml.Matrix4f;

/**
 * Клиентский рендерер рулетки - привязывается к голове игрока
 * Рулетка с красными, чёрными и 1 зелёным сегментом
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HaloClientRenderer {

    // Параметры рулетки
    private static final float ROULETTE_RADIUS = 0.45f;
    private static final float ROULETTE_HEIGHT = 0.5f; // Над головой
    private static final int TOTAL_SEGMENTS = 37; // 18 красных + 18 чёрных + 1 зелёный
    private static final int RED_SEGMENTS = 18;
    private static final int BLACK_SEGMENTS = 18;
    private static final int GREEN_SEGMENTS = 1;

    // Анимация вращения
    private static float rotation = 0;
    private static final float ROTATION_SPEED = 2.0f;

    // Цвета для рулетки
    private static final int COLOR_RED = 0xFF0000;
    private static final int COLOR_BLACK = 0x000000;
    private static final int COLOR_GREEN = 0x00FF00;
    private static final int COLOR_GOLD_BORDER = 0xFFD700; // Золотая рамка

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();

        // Проверяем, что косметика должна отображаться
        if (!shouldRenderRoulette(player)) return;

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int packedLight = event.getPackedLight();

        renderRoulette(poseStack, bufferSource, packedLight, player, event.getPartialTick());
    }

    private static void renderRoulette(PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, Player player, float partialTick) {
        poseStack.pushPose();

        // Позиционируем над головой (с учётом позы игрока)
        float yOffset = player.getBbHeight() + ROULETTE_HEIGHT;

        // Учитываем приседание
        if (player.isCrouching()) {
            yOffset -= 0.2f;
        }

        poseStack.translate(0, yOffset, 0);

        // Анимация вращения
        rotation += ROTATION_SPEED * partialTick;
        if (rotation >= 360) rotation -= 360;
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotation));

        // Рендерим рулетку
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.debugQuads());
        Matrix4f matrix = poseStack.last().pose();

        // Массив цветов для сегментов (красный, чёрный, чередование + 1 зелёный в конце)
        int[] colors = new int[TOTAL_SEGMENTS];

        // Заполняем чередующимися цветами
        for (int i = 0; i < RED_SEGMENTS + BLACK_SEGMENTS; i++) {
            colors[i] = (i % 2 == 0) ? COLOR_RED : COLOR_BLACK;
        }

        // Последний сегмент - зелёный
        colors[TOTAL_SEGMENTS - 1] = COLOR_GREEN;

        // Рисуем сегменты рулетки
        float angleStep = 360f / TOTAL_SEGMENTS;

        for (int i = 0; i < TOTAL_SEGMENTS; i++) {
            float startAngle = i * angleStep;
            float endAngle = (i + 1) * angleStep;

            int color = colors[i];
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;

            // Рисуем треугольник от центра к краям
            drawSegment(matrix, vertexConsumer, startAngle, endAngle, r, g, b, packedLight);
        }

        // Рисуем золотую рамку по краю
        drawBorder(matrix, vertexConsumer, packedLight);

        poseStack.popPose();
    }

    /**
     * Рисует один сегмент рулетки
     */
    private static void drawSegment(Matrix4f matrix, VertexConsumer consumer,
                                    float startAngle, float endAngle,
                                    float r, float g, float b, int light) {
        // Конвертируем углы в радианы
        float startRad = (float) Math.toRadians(startAngle);
        float endRad = (float) Math.toRadians(endAngle);

        // Вычисляем точки на окружности
        float x1 = ROULETTE_RADIUS * Mth.cos(startRad);
        float z1 = ROULETTE_RADIUS * Mth.sin(startRad);
        float x2 = ROULETTE_RADIUS * Mth.cos(endRad);
        float z2 = ROULETTE_RADIUS * Mth.sin(endRad);

        // Треугольник от центра к двум точкам на окружности
        // Центр
        consumer.vertex(matrix, 0, 0.01f, 0)
                .color(r, g, b, 1.0f)
                .uv(0.5f, 0.5f)
                .overlayCoords(0)
                .uv2(240, 240) // Полная яркость
                .normal(0, 1, 0)
                .endVertex();

        // Первая точка на окружности
        consumer.vertex(matrix, x1, 0.01f, z1)
                .color(r, g, b, 1.0f)
                .uv(0, 0)
                .overlayCoords(0)
                .uv2(240, 240)
                .normal(0, 1, 0)
                .endVertex();

        // Вторая точка на окружности
        consumer.vertex(matrix, x2, 0.01f, z2)
                .color(r, g, b, 1.0f)
                .uv(1, 0)
                .overlayCoords(0)
                .uv2(240, 240)
                .normal(0, 1, 0)
                .endVertex();

        // Четвёртая вершина для quad (повторяем последнюю)
        consumer.vertex(matrix, x2, 0.01f, z2)
                .color(r, g, b, 1.0f)
                .uv(1, 0)
                .overlayCoords(0)
                .uv2(240, 240)
                .normal(0, 1, 0)
                .endVertex();
    }

    /**
     * Рисует золотую рамку по краю рулетки
     */
    private static void drawBorder(Matrix4f matrix, VertexConsumer consumer, int light) {
        float borderWidth = 0.03f; // Толщина рамки
        float innerRadius = ROULETTE_RADIUS - borderWidth;

        float r = ((COLOR_GOLD_BORDER >> 16) & 0xFF) / 255f;
        float g = ((COLOR_GOLD_BORDER >> 8) & 0xFF) / 255f;
        float b = (COLOR_GOLD_BORDER & 0xFF) / 255f;

        int segments = 32;
        for (int i = 0; i < segments; i++) {
            float angle1 = (float) (2 * Math.PI * i / segments);
            float angle2 = (float) (2 * Math.PI * (i + 1) / segments);

            float x1_inner = innerRadius * Mth.cos(angle1);
            float z1_inner = innerRadius * Mth.sin(angle1);
            float x2_inner = innerRadius * Mth.cos(angle2);
            float z2_inner = innerRadius * Mth.sin(angle2);

            float x1_outer = ROULETTE_RADIUS * Mth.cos(angle1);
            float z1_outer = ROULETTE_RADIUS * Mth.sin(angle1);
            float x2_outer = ROULETTE_RADIUS * Mth.cos(angle2);
            float z2_outer = ROULETTE_RADIUS * Mth.sin(angle2);

            // Квад для рамки
            consumer.vertex(matrix, x1_inner, 0.02f, z1_inner)
                    .color(r, g, b, 1.0f).uv(0, 0).overlayCoords(0)
                    .uv2(240, 240).normal(0, 1, 0).endVertex();

            consumer.vertex(matrix, x1_outer, 0.02f, z1_outer)
                    .color(r, g, b, 1.0f).uv(0, 1).overlayCoords(0)
                    .uv2(240, 240).normal(0, 1, 0).endVertex();

            consumer.vertex(matrix, x2_outer, 0.02f, z2_outer)
                    .color(r, g, b, 1.0f).uv(1, 1).overlayCoords(0)
                    .uv2(240, 240).normal(0, 1, 0).endVertex();

            consumer.vertex(matrix, x2_inner, 0.02f, z2_inner)
                    .color(r, g, b, 1.0f).uv(1, 0).overlayCoords(0)
                    .uv2(240, 240).normal(0, 1, 0).endVertex();
        }
    }

    /**
     * Проверяет, должна ли отображаться рулетка для игрока
     */
    private static boolean shouldRenderRoulette(Player player) {
        // Проверка на режим наблюдателя
        if (isSpectator(player)) return false;

        // Проверка на жив ли игрок
        if (!player.isAlive() || player.getHealth() <= 0) return false;

        // Для локального игрока проверяем через ClientPlayerData
        if (player == Minecraft.getInstance().player) {
            CosmeticData cosmeticData = ClientPlayerData.getCosmeticData();
            return cosmeticData.isEnabled("halo") && cosmeticData.hasPurchased("halo");
        }

        // Для других игроков проверяем через кэш
        return ClientCosmeticCache.hasCosmetic(player, "halo");
    }

    /**
     * Проверка на режим наблюдателя
     */
    private static boolean isSpectator(Player player) {
        if (player == Minecraft.getInstance().player) {
            return Minecraft.getInstance().gameMode != null
                    && Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR;
        }

        if (player instanceof AbstractClientPlayer clientPlayer) {
            return clientPlayer.isSpectator();
        }

        return player.isSpectator();
    }
}