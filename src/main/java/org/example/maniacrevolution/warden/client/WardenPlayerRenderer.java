package org.example.maniacrevolution.warden.client;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/** Uses the actual player's visibility, team, outline and name rules. Hitbox/camera are untouched. */
public final class WardenPlayerRenderer extends LivingEntityRenderer<AbstractClientPlayer, WardenPlayerModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/entity/warden/warden.png");

    public WardenPlayerRenderer(EntityRendererProvider.Context context) {
        super(context, new WardenPlayerModel(context.bakeLayer(ModelLayers.WARDEN)), 0.9F);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) { return TEXTURE; }

    @Override
    protected void setupRotations(AbstractClientPlayer player, PoseStack pose, float age, float yaw, float partial) {
        super.setupRotations(player, pose, age, yaw, partial);
        float swim = player.getSwimAmount(partial);
        if (swim > 0 && !player.isFallFlying()) {
            pose.mulPose(Axis.XP.rotationDegrees(swim * (player.isInWater() ? -90 - player.getXRot() : -90)));
            if (player.isVisuallySwimming()) pose.translate(0, -1, 0.3);
        }
    }

    @Override
    protected boolean shouldShowName(AbstractClientPlayer player) {
        return !WardenPlayerForm.isPreview(player) && super.shouldShowName(player);
    }
}
