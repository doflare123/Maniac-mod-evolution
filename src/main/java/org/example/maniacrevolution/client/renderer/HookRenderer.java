package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.client.model.HookModel;
import org.example.maniacrevolution.entity.HookEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class HookRenderer extends EntityRenderer<HookEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(Maniacrev.MODID, "textures/entity/hook.png");

    private final HookModel<HookEntity> model;

    public HookRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new HookModel<>(context.bakeLayer(HookModel.LAYER_LOCATION));
    }

    @Override
    public void render(HookEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

        // Сначала рендерим цепь
        renderChain(entity, partialTicks, poseStack, buffer, packedLight);

        poseStack.pushPose();

        // Вращение вокруг своей оси для крутого эффекта
        float rotation = (entity.tickCount + partialTicks) * 20.0f; // Скорость вращения
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));

        // Сдвигаем модель для правильного центрирования
        poseStack.translate(0, -1.5, 0);

        // Рендерим модель хука
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        this.model.renderToBuffer(poseStack, vertexConsumer, packedLight,
                OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderChain(HookEntity hook, float partialTicks,
                             PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        Player player = hook.getOwner() instanceof Player p ? p : null;
        if (player == null) return;

        poseStack.pushPose();

        // Позиция игрока (рука с интерполяцией)
        double playerX = Mth.lerp(partialTicks, player.xOld, player.getX());
        double playerY = Mth.lerp(partialTicks, player.yOld, player.getY()) + player.getEyeHeight() - 0.3;
        double playerZ = Mth.lerp(partialTicks, player.zOld, player.getZ());

        // Позиция хука с интерполяцией
        double hookX = Mth.lerp(partialTicks, hook.xOld, hook.getX());
        double hookY = Mth.lerp(partialTicks, hook.yOld, hook.getY());
        double hookZ = Mth.lerp(partialTicks, hook.zOld, hook.getZ());

        // Вектор от хука к игроку
        double dx = playerX - hookX;
        double dy = playerY - hookY;
        double dz = playerZ - hookZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // Нормализуем направление
        dx /= distance;
        dy /= distance;
        dz /= distance;

        // Вычисляем углы
        float yaw = (float) (Mth.atan2(dx, dz) * (180.0 / Math.PI));
        float pitch = (float) (Mth.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180.0 / Math.PI));

        // Применяем повороты
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));

        // Используем debug line strip для видимой линии
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.leash());
        Matrix4f matrix4f = poseStack.last().pose();

        // Рисуем цепь сегментами
        int segments = Math.max(2, (int)(distance * 5));

        for (int i = 0; i < segments; i++) {
            float t = (float) i / segments;
            float z = (float) (distance * t);

            // Вычисляем цвет с градиентом
            int brightness = 255 - (int)(t * 50); // Немного темнее к концу

            vertexConsumer.vertex(matrix4f, 0.0f, 0.0f, z)
                    .color(brightness, brightness, brightness, 255)
                    .uv2(packedLight)
                    .endVertex();
        }

        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(HookEntity entity) {
        return TEXTURE;
    }
}