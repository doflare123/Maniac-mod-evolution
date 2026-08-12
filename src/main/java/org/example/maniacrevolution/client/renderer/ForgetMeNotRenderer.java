package org.example.maniacrevolution.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.entity.ForgetMeNotEntity;

/** Рисует установленную незабудку как маленькую насыщенную объёмную группу спрайтов. */
public final class ForgetMeNotRenderer extends EntityRenderer<ForgetMeNotEntity> {
    private static final float MAIN_SCALE = 0.72F;
    private static final float SIDE_SCALE = 0.48F;
    private static final float SECOND_PLANE_ROTATION_DEGREES = 90.0F;
    private static final float LEFT_ROTATION_DEGREES = -28.0F;
    private static final float RIGHT_ROTATION_DEGREES = 32.0F;
    private static final double MAIN_HEIGHT_OFFSET = 0.38D;
    private static final double SIDE_HEIGHT_OFFSET = 0.22D;
    private static final double SIDE_HORIZONTAL_OFFSET = 0.20D;

    private final ItemRenderer itemRenderer;
    private final ItemStack flowerStack;

    public ForgetMeNotRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
        flowerStack = new ItemStack(ModItems.FORGET_ME_NOT.get());
        shadowRadius = 0.0F;
    }

    @Override
    public void render(ForgetMeNotEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        renderSprite(entity, poseStack, buffer, packedLight,
                0.0D, MAIN_HEIGHT_OFFSET, 0.0D, 0.0F, MAIN_SCALE, 0);
        renderSprite(entity, poseStack, buffer, packedLight,
                0.0D, MAIN_HEIGHT_OFFSET, 0.0D,
                SECOND_PLANE_ROTATION_DEGREES, MAIN_SCALE, 1);
        renderSprite(entity, poseStack, buffer, packedLight,
                -SIDE_HORIZONTAL_OFFSET, SIDE_HEIGHT_OFFSET, 0.04D,
                LEFT_ROTATION_DEGREES, SIDE_SCALE, 2);
        renderSprite(entity, poseStack, buffer, packedLight,
                SIDE_HORIZONTAL_OFFSET, SIDE_HEIGHT_OFFSET, -0.04D,
                RIGHT_ROTATION_DEGREES, SIDE_SCALE, 3);
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    private void renderSprite(ForgetMeNotEntity entity, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight,
                              double x, double y, double z,
                              float rotationDegrees, float scale, int seedOffset) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poseStack.scale(scale, scale, scale);
        itemRenderer.renderStatic(
                flowerStack,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId() + seedOffset
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ForgetMeNotEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
