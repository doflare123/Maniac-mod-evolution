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
import org.example.maniacrevolution.entity.RedColorCardProjectile;

/** Renders only the thrown red card sideways; the held item keeps its normal transform. */
public final class RedColorCardProjectileRenderer
        extends EntityRenderer<RedColorCardProjectile> {
    private static final double MIN_CAMERA_DISTANCE_SQUARED = 12.25D;
    private final ItemRenderer itemRenderer;

    public RedColorCardProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(RedColorCardProjectile projectile, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (projectile.tickCount < 2
                && this.entityRenderDispatcher.camera.getEntity()
                .distanceToSqr(projectile) < MIN_CAMERA_DISTANCE_SQUARED) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        this.itemRenderer.renderStatic(projectile.getItem(), ItemDisplayContext.GROUND,
                packedLight, OverlayTexture.NO_OVERLAY, poseStack, buffer,
                projectile.level(), projectile.getId());
        poseStack.popPose();

        super.render(projectile, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RedColorCardProjectile projectile) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
